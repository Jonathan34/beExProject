package com.bedroid.beEx;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
//import android.app.Activity;
import android.accounts.AccountAuthenticatorActivity;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.CheckBox;

/*import com.independentsoft.exchange.Appointment;
import com.independentsoft.exchange.AppointmentPropertyPath;
import com.independentsoft.exchange.CalendarView;
import com.independentsoft.exchange.FindItemResponse;
import com.independentsoft.exchange.InstanceType;
import com.independentsoft.exchange.RecurringMasterItemId;
import com.independentsoft.exchange.Service;
import com.independentsoft.exchange.ServiceException;
import com.independentsoft.exchange.StandardFolder;
*/
import com.bedroid.beEx.helper.CalendarHelper;
import com.bedroid.beEx.helper.ExchangeHelper;

import org.apache.commons.httpclient.Cookie;

import java.net.URI;
import java.net.URISyntaxException;
import java.text.SimpleDateFormat;
import java.util.Date;
/*import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;*/

import microsoft.exchange.webservices.data.Appointment;
import microsoft.exchange.webservices.data.AutodiscoverService;
import microsoft.exchange.webservices.data.BasePropertySet;
import microsoft.exchange.webservices.data.BodyType;
import microsoft.exchange.webservices.data.CalendarFolder;
import microsoft.exchange.webservices.data.CalendarView;
import microsoft.exchange.webservices.data.ExchangeCredentials;
import microsoft.exchange.webservices.data.ExchangeServerInfo;
import microsoft.exchange.webservices.data.ExchangeService;
import microsoft.exchange.webservices.data.ExchangeVersion;
import microsoft.exchange.webservices.data.FindFoldersResults;
import microsoft.exchange.webservices.data.FindItemResponse;
import microsoft.exchange.webservices.data.FindItemsResults;
import microsoft.exchange.webservices.data.Folder;
import microsoft.exchange.webservices.data.FolderView;
import microsoft.exchange.webservices.data.MessageBody;
import microsoft.exchange.webservices.data.PropertySet;
import microsoft.exchange.webservices.data.WebCredentials;
import microsoft.exchange.webservices.data.WellKnownFolderName;

/**
 * Activity which displays a login screen to the user, offering registration as
 * well.
 */
public class LoginActivity extends AccountAuthenticatorActivity {

    private static final String TAG = "LoginActivity";

    /**
     * Keep track of the login task to ensure we can cancel it if requested.
     */
    private UserLoginTask mAuthTask = null;

    // Values for email and password at the time of the login attempt.
    private String mEmail;
    private String mPassword;
    private String mServerUrl;
    // https://mail.domain.com/EWS/Exchange.asmx .
    // see http://nuanceimaging.custhelp.com/app/answers/detail/a_id/13098
    private String mServerPort;
    private boolean mServerSSL;

    // UI references.
    private EditText mEmailView;
    private EditText mPasswordView;
    private EditText mServerUrlView;
    private EditText mServerPortView;
    private CheckBox mServerSSLView;
    private View mLoginFormView;
    private View mLoginStatusView;
    private TextView mLoginStatusMessageView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_login);

        // Set up the login form.
        //Email
        mEmailView = (EditText) findViewById(R.id.email);
        mEmailView.setText("JDelfour@slb.com");

        //Password
        mPasswordView = (EditText) findViewById(R.id.password);
        mPasswordView.setText("");
        mPasswordView.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int id, KeyEvent keyEvent) {
                if (id == R.id.login || id == EditorInfo.IME_NULL) {
                    attemptLogin();
                    return true;
                }
                return false;
            }
        });

        //View
        mLoginFormView = findViewById(R.id.login_form);
        mLoginStatusView = findViewById(R.id.login_status);
        mLoginStatusMessageView = (TextView) findViewById(R.id.login_status_message);

        findViewById(R.id.sign_in_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                attemptLogin();
            }
        });

        //Server URL
        mServerUrlView = (EditText) findViewById(R.id.serverUrlText);
        mServerUrlView.setText("https://mobile.slb.com/EWS/Exchange.asmx");
        //Server Port
        mServerPortView = (EditText) findViewById(R.id.serverPortNumber);

        //Server SSL
        mServerSSLView = (CheckBox) findViewById(R.id.checkBoxSSL);
        mServerSSLView.setChecked(true);
        //if (mServerSSLView.isChecked()) {
        //    mServerSSLView.setChecked(false);
        //}
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.login, menu);
        return true;
    }

    /**
     * Attempts to sign in or register the account specified by the login form.
     * If there are form errors (invalid email, missing fields, etc.), the
     * errors are presented and no actual login attempt is made.
     */
    public void attemptLogin() {
        if (mAuthTask != null) {
            return;
        }

        // Reset errors.
        mEmailView.setError(null);
        mPasswordView.setError(null);
        mServerUrlView.setError(null);
        mServerPortView.setError(null);
        mServerSSLView.setError(null);

        // Store values at the time of the login attempt.
        mEmail      = mEmailView.getText().toString();
        mPassword   = mPasswordView.getText().toString();
        mServerPort = mServerPortView.getText().toString();
        mServerUrl  = mServerUrlView.getText().toString();
        mServerSSL  = mServerSSLView.isChecked();

        boolean cancel = false;
        View focusView = null;

        // Check for a valid password.
        if (TextUtils.isEmpty(mPassword)) {
            mPasswordView.setError(getString(R.string.error_field_required));
            focusView = mPasswordView;
            cancel = true;
        } else if (mPassword.length() < 4) {
            mPasswordView.setError(getString(R.string.error_invalid_password));
            focusView = mPasswordView;
            cancel = true;
        }

        // Check for a valid email address.
        if (TextUtils.isEmpty(mEmail)) {
            mEmailView.setError(getString(R.string.error_field_required));
            focusView = mEmailView;
            cancel = true;
        } else if (!mEmail.contains("@")) {
            mEmailView.setError(getString(R.string.error_invalid_email));
            focusView = mEmailView;
            cancel = true;
        }

        // Check for a valid server
        if (TextUtils.isEmpty(mServerUrl)) {
            mServerUrlView.setError(getString(R.string.error_field_required));
            focusView = mServerUrlView;
            cancel = true;
        } else if (!mServerUrl.startsWith("http") && !mServerUrl.startsWith("https")) {
            String prefix = "http://";
            if(mServerSSL == true)
                prefix = "https://";
            mServerUrl = prefix + mServerUrl;
        }

        if (cancel) {
            // There was an error; don't attempt login and focus the first
            // form field with an error.
            focusView.requestFocus();
        } else {
            // Show a progress spinner, and kick off a background task to
            // perform the user login attempt.
            mLoginStatusMessageView.setText(R.string.login_progress_signing_in);
            showProgress(true);
            mAuthTask = new UserLoginTask();
            mAuthTask.execute((Void) null);
        }
    }

    /**
     * Shows the progress UI and hides the login form.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
    private void showProgress(final boolean show) {
        // On Honeycomb MR2 we have the ViewPropertyAnimator APIs, which allow
        // for very easy animations. If available, use these APIs to fade-in
        // the progress spinner.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
            int shortAnimTime = getResources().getInteger(android.R.integer.config_shortAnimTime);

            mLoginStatusView.setVisibility(View.VISIBLE);
            mLoginStatusView.animate()
                    .setDuration(shortAnimTime)
                    .alpha(show ? 1 : 0)
                    .setListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            mLoginStatusView.setVisibility(show ? View.VISIBLE : View.GONE);
                        }
                    });

            mLoginFormView.setVisibility(View.VISIBLE);
            mLoginFormView.animate()
                    .setDuration(shortAnimTime)
                    .alpha(show ? 0 : 1)
                    .setListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
                        }
                    });
        } else {
            // The ViewPropertyAnimator APIs are not available, so simply show
            // and hide the relevant UI components.
            mLoginStatusView.setVisibility(show ? View.VISIBLE : View.GONE);
            mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
        }
    }

    /**
     * Represents an asynchronous login/registration task used to authenticate
     * the user.
     */
    public class UserLoginTask extends AsyncTask<Void, Void, Boolean> {


        @Override
        protected Boolean doInBackground(Void... params) {
            // attempt authentication against the network service.
            /************************************************************/
            try {
                /*ExchangeService service = new ExchangeService(ExchangeVersion.Exchange2010_SP2);
                ExchangeCredentials credentials = new WebCredentials(mEmail, mPassword);
                service.setCredentials(credentials);
                URI url = new URI(mServerUrl);
                service.setUrl(url);*/
                //ExchangeService service = ExchangeHelper.getInstance().connectToExchange(mServerUrl, mEmail, mPassword);
                //System.out.println(service.getUrl().toString());

                //CALENDAR
                //ExchangeHelper.getInstance().getCalendarTest();

                //EMAIL
                /*FindFoldersResults findResults = service.findFolders(WellKnownFolderName.Inbox, new FolderView(Integer.MAX_VALUE));

                for(Folder folder : findResults.getFolders())
                {
                    System.out.println("Count======"+folder.getChildFolderCount());
                    System.out.println("Name======="+folder.getDisplayName());
                }*/
            /*} catch (URISyntaxException e) {
                e.printStackTrace();*/
            } catch (Exception e) {
                e.printStackTrace();
            }

            /*for (String credential : DUMMY_CREDENTIALS) {
                String[] pieces = credential.split(":");
                if (pieces[0].equals(mEmail)) {
                    // Account exists, return true if the password matches.
                    return pieces[1].equals(mPassword);
                }
            }*/

            //Retrieve if the account exists and return it
            AccountManager am = AccountManager.get(getBaseContext());
            Account[] accounts = am.getAccountsByType("com.bedroid.beEx.account");
            if(accounts != null && accounts.length > 0) {
                Log.i(TAG, "The account already exists");
                //TODO handle several accounts
                return true;
            }

            //Register a new account and associate a calendar to it
            Account account = new Account(mEmail, "com.bedroid.beEx.account");
            if (am.addAccountExplicitly(account, mPassword, null)) {
                String calId = CalendarHelper.addCalendar(account, getBaseContext().getContentResolver());
                Log.i(TAG, "Adding calendar " + calId + " with url " + mServerUrl);
                am.setUserData(account, "CALENDAR_ID", calId);
                am.setUserData(account, "ADAPTER_TYPE", CalendarHelper.CalendarType.EXCHANGE_CALENDAR.toString());
                am.setUserData(account, "SERVER_URL", mServerUrl);
            }

            return true;
        }

        @Override
        protected void onPostExecute(final Boolean success) {
            mAuthTask = null;
            showProgress(false);

            if (success) {
                finish();
            } else {
                mPasswordView.setError(getString(R.string.error_incorrect_password));
                mPasswordView.requestFocus();
            }
        }

        @Override
        protected void onCancelled() {
            mAuthTask = null;
            showProgress(false);
        }
    }
}
