package sk.henrichg.phoneprofilesplus;

import android.content.ContentUris;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.TypedArray;
import android.database.Cursor;
import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;
import android.provider.ContactsContract;
import android.util.AttributeSet;

import androidx.preference.DialogPreference;

import java.util.ArrayList;
import java.util.List;

public class ContactsMultiSelectDialogPreference extends DialogPreference
{
    ContactsMultiSelectDialogPreferenceFragment fragment;

    private final Context _context;

    private final boolean withoutNumbers;

    String value = "";
    private String defaultValue;
    private boolean savedInstanceState;

    List<Contact> contactList;

    public ContactsMultiSelectDialogPreference(Context context, AttributeSet attrs) {
        super(context, attrs);

        _context = context;

        //noinspection resource
        TypedArray locationGeofenceType = context.obtainStyledAttributes(attrs,
                R.styleable.PPContactsMultiSelectDialogPreference, 0, 0);
        withoutNumbers = locationGeofenceType.getBoolean(R.styleable.PPContactsMultiSelectDialogPreference_withoutNumbers, false);

        locationGeofenceType.recycle();

        //if (PhoneProfilesService.getContactsCache() == null)
        //    PhoneProfilesService.createContactsCache();

    }

    @Override
    protected void onSetInitialValue(Object defaultValue)
    {
        // Get the persistent value
        value = getPersistedString((String)defaultValue);
        PPApplicationStatic.logE("[CONTACTS_DIALOG] ContactsMultiSelectDialogPreference.onSetInitialValue", "value="+value);
        this.defaultValue = (String)defaultValue;
        //getValueCMSDP(); // toto cita z cache, je tam blokoanie mutexom
        setSummaryCMSDP(); // toto cita z databazy, ak je len jedne kontakt nastaveny
    }

    void refreshListView(@SuppressWarnings("SameParameterValue") final boolean notForUnselect) {
        if (fragment != null)
            fragment.refreshListView(notForUnselect);
    }

    void getValueCMSDP()
    {
        // change checked state by value
        ContactsCache contactsCache = PPApplicationStatic.getContactsCache();
        if (contactsCache == null)
            return;

        synchronized (PPApplication.contactsCacheMutex) {
            List<Contact>  localContactList = contactsCache.getList(/*withoutNumbers*/);
            if (localContactList != null) {
                contactList = new ArrayList<>();
                if (!withoutNumbers) {
                    for (Contact contact : localContactList) {
                        if (contact.phoneId != 0)
                            contactList.add(contact);
                    }
                } else
                    contactList.addAll(localContactList);
                String[] splits = value.split(StringConstants.STR_SPLIT_REGEX);
                for (Contact contact : contactList) {
                    if (withoutNumbers || (contact.phoneId != 0)) {
                        contact.checked = false;
                        for (String split : splits) {
                            try {
                                String[] splits2 = split.split("#");
                                long contactId = Long.parseLong(splits2[0]);
                                if (withoutNumbers) {
                                    if (contact.contactId == contactId)
                                        contact.checked = true;
                                }
                                else {
                                    long phoneId = Long.parseLong(splits2[1]);
                                    if ((contact.contactId == contactId) && (contact.phoneId == phoneId))
                                        contact.checked = true;
                                }
                            } catch (Exception e) {
                                //PPApplicationStatic.recordException(e);
                            }
                        }
                    }

                    contact.photoUri = getPhotoUri(contact.contactId);

                    boolean found = false;
                    String accountType = "";
                    PackageManager packageManager = _context.getPackageManager();
                    try {
                        ApplicationInfo applicationInfo = packageManager.getApplicationInfo(contact.accountType, PackageManager.MATCH_ALL);
                        if (applicationInfo != null) {
                            accountType = packageManager.getApplicationLabel(applicationInfo).toString();
                            found = true;
                        }
                    } catch (Exception ignored) {}
                    if (!found) {
                        if (contact.accountType.equals("com.osp.app.signin"))
                            accountType = _context.getString(R.string.contact_account_type_samsung_account);
                        if (contact.accountType.equals("com.google"))
                            accountType = _context.getString(R.string.contact_account_type_google_account);
                        if (contact.accountType.equals("vnd.sec.contact.sim"))
                            accountType = _context.getString(R.string.contact_account_type_sim_card);
                        if (contact.accountType.equals("vnd.sec.contact.sim2"))
                            accountType = _context.getString(R.string.contact_account_type_sim_card);
                        if (contact.accountType.equals("vnd.sec.contact.phone"))
                            accountType = _context.getString(R.string.contact_account_type_phone_application);
                        if (contact.accountType.equals("org.thoughtcrime.securesms"))
                            accountType = "Signal";
                        if (contact.accountType.equals("com.google.android.apps.tachyon"))
                            accountType = "Duo";
                        if (contact.accountType.equals("com.whatsapp"))
                            accountType = "WhatsApp";
                    }
                    if ((!accountType.isEmpty()) &&
                            (!contact.accountType.equals("vnd.sec.contact.sim")) &&
                            (!contact.accountType.equals("vnd.sec.contact.sim2")) &&
                            (!contact.accountType.equals("vnd.sec.contact.phone")) &&
                            (!contact.accountName.equals(accountType)))
                        accountType = accountType + StringConstants.CHAR_NEW_LINE+"  - " + contact.accountName;
                    contact.displayedAccountType = accountType;
                }
                // move checked on top
                int i = 0;
                int ich = 0;
                while (i < contactList.size()) {
                    Contact contact = contactList.get(i);
                    if (contact.checked) {
                        contactList.remove(i);
                        contactList.add(ich, contact);
                        ich++;
                    }
                    i++;
                }
            }
        }
    }

    static String getSummary(String value, boolean withoutNumbers, Context context) {
        String summary = context.getString(R.string.contacts_multiselect_summary_text_not_selected);
        if (Permissions.checkContacts(context)) {
            if (!value.isEmpty()) {
                String[] splits = value.split(StringConstants.STR_SPLIT_REGEX);
                if (splits.length == 1) {
                    boolean found = false;
                    String[] projection = new String[]{
                            ContactsContract.Contacts._ID,
                            ContactsContract.Contacts.DISPLAY_NAME,
                            ContactsContract.Contacts.PHOTO_ID};
                    String[] splits2 = splits[0].split("#");
                    String selection = ContactsContract.Contacts._ID + "=" + splits2[0];
                    Cursor mCursor = context.getContentResolver().query(ContactsContract.Contacts.CONTENT_URI, projection, selection, null, null);

                    if (mCursor != null) {
                        while (mCursor.moveToNext()) {
                            selection = ContactsContract.CommonDataKinds.Phone.CONTACT_ID + "=" + splits2[0] + " AND " +
                                    ContactsContract.CommonDataKinds.Phone._ID + "=" + splits2[1];
                            Cursor phones = context.getContentResolver().query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null, selection, null, null);
                            if (phones != null) {
                                //while (phones.moveToNext()) {
                                if (phones.moveToFirst()) {
                                    found = true;
                                    summary = mCursor.getString(mCursor.getColumnIndexOrThrow(ContactsContract.Contacts.DISPLAY_NAME));
                                    if (!withoutNumbers)
                                        summary = summary + StringConstants.CHAR_NEW_LINE + phones.getString(phones.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER));
                                    //break;
                                }
                                phones.close();
                            }
                            if (found)
                                break;
                        }
                        mCursor.close();
                    }
                    if (!found)
                        summary = context.getString(R.string.contacts_multiselect_summary_text_selected) + StringConstants.STR_COLON_WITH_SPACE + splits.length;
                } else
                    summary = context.getString(R.string.contacts_multiselect_summary_text_selected) + StringConstants.STR_COLON_WITH_SPACE + splits.length;
            }
        }
        return summary;
    }

    private void setSummaryCMSDP()
    {
        setSummary(getSummary(value, withoutNumbers, _context));
    }

    private void getValue() {
        // fill with strings of contacts separated with |
        value = "";
        StringBuilder _value = new StringBuilder();
        if (contactList != null)
        {
            for (Contact contact : contactList)
            {
                if (contact.checked)
                {
                    //if (!value.isEmpty())
                    //    value = value + "|";
                    //value = value + contact.contactId + "#" + contact.phoneId;
                    if (_value.length() > 0)
                        _value.append("|");
                    _value.append(contact.contactId).append("#").append(contact.phoneId);
                }
            }
        }
        value = _value.toString();
    }

    void persistValue() {
        if (shouldPersist())
        {
            getValue();
            PPApplicationStatic.logE("[CONTACTS_DIALOG] ContactsMultiSelectDialogPreference.persistValue", "value="+value);
            persistString(value);

            setSummaryCMSDP();
        }
    }

    void resetSummary() {
        if (!savedInstanceState) {
            value = getPersistedString(defaultValue);
            setSummaryCMSDP();
        }
        savedInstanceState = false;
    }

    @Override
    protected Parcelable onSaveInstanceState()
    {
        savedInstanceState = true;

        final Parcelable superState = super.onSaveInstanceState();
        /*if (isPersistent()) {
            return superState;
        }*/

        final ContactsMultiSelectDialogPreference.SavedState myState = new ContactsMultiSelectDialogPreference.SavedState(superState);
        getValue();
        myState.value = value;
        myState.defaultValue = defaultValue;

        return myState;
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state)
    {
        //if (dataWrapper == null)
        //    dataWrapper = new DataWrapper(prefContext, false, 0, false);

        if ((state == null) || (!state.getClass().equals(ContactsMultiSelectDialogPreference.SavedState.class))) {
            // Didn't save state for us in onSaveInstanceState
            super.onRestoreInstanceState(state);
            setSummaryCMSDP();
            return;
        }

        // restore instance state
        ContactsMultiSelectDialogPreference.SavedState myState = (ContactsMultiSelectDialogPreference.SavedState)state;
        super.onRestoreInstanceState(myState.getSuperState());
        value = myState.value;
        defaultValue = myState.defaultValue;

        //getValueCMSDP();
        setSummaryCMSDP();
        refreshListView(true);
        //notifyChanged();
    }

    // SavedState class
    private static class SavedState extends BaseSavedState
    {
        String value;
        String defaultValue;

        SavedState(Parcel source)
        {
            super(source);

            value = source.readString();
            defaultValue = source.readString();
        }

        @Override
        public void writeToParcel(Parcel dest, int flags)
        {
            super.writeToParcel(dest, flags);

            dest.writeString(value);
            dest.writeString(defaultValue);
        }

        SavedState(Parcelable superState)
        {
            super(superState);
        }

        public static final Creator<ContactsMultiSelectDialogPreference.SavedState> CREATOR =
                new Creator<ContactsMultiSelectDialogPreference.SavedState>() {
                    public ContactsMultiSelectDialogPreference.SavedState createFromParcel(Parcel in)
                    {
                        return new ContactsMultiSelectDialogPreference.SavedState(in);
                    }
                    public ContactsMultiSelectDialogPreference.SavedState[] newArray(int size)
                    {
                        return new ContactsMultiSelectDialogPreference.SavedState[size];
                    }

                };

    }

    /**
     * @return the photo URI
     */
    private Uri getPhotoUri(long contactId)
    {
    /*    try {
            Cursor cur = context.getContentResolver().query(ContactsContract.Data.CONTENT_URI, null,
                            ContactsContract.Data.CONTACT_ID + "=" + photoId + " AND "
                            + ContactsContract.Data.MIMETYPE + "='"
                            + ContactsContract.CommonDataKinds.Photo.CONTENT_ITEM_TYPE + "'", null,
                            null);
            if (cur != null)
            {
                if (!cur.moveToFirst())
                {
                    return null; // no photo
                }
            }
            else
                return null; // error in cursor process
        } catch (Exception e) {
            return null;
        }
        */
        Uri person = ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, contactId);
        return Uri.withAppendedPath(person, ContactsContract.Contacts.Photo.CONTENT_DIRECTORY);
    }

}
