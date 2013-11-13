package com.bedroid.beEx.authsync;

import com.bedroid.beEx.LoginActivity;

import android.accounts.AbstractAccountAuthenticator;
import android.accounts.Account;
import android.accounts.AccountAuthenticatorResponse;
import android.accounts.AccountManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;

import java.net.URI;

import microsoft.exchange.webservices.data.ExchangeCredentials;
import microsoft.exchange.webservices.data.ExchangeService;
import microsoft.exchange.webservices.data.ExchangeVersion;
import microsoft.exchange.webservices.data.WebCredentials;

/**
 * This class is an implementation of AbstractAccountAuthenticator for
 * authenticating accounts in the com.example.android.samplesync domain. The
 * interesting thing that this class demonstrates is the use of authTokens as
 * part of the authentication process. In the account setup UI, the user enters
 * their username and password. But for our subsequent calls off to the service
 * for syncing, we want to use an authtoken instead - so we're not continually
 * sending the password over the wire. getAuthToken() will be called when
 * SyncAdapter calls AccountManager.blockingGetAuthToken(). When we get called,
 * we need to return the appropriate authToken for the specified account. If we
 * already have an authToken stored in the account, we return that authToken. If
 * we don't, but we do have a username and password, then we'll attempt to talk
 * to the sample service to fetch an authToken. If that fails (or we didn't have
 * a username/password), then we need to prompt the user - so we create an
 * AuthenticatorActivity intent and return that. That will display the dialog
 * that prompts the user for their login information.
 */

public class Authenticator extends AbstractAccountAuthenticator {
    public static final String AUTHTOKEN_TYPE = "com.bedroid.beEx.authsync";
    public static final String ACCOUNT_TYPE   = "com.bedroid.beEx.account";
    private final Context mContext;

    public Authenticator(Context context) {
        super(context);
        this.mContext = context;
    }

    @Override
    public Bundle addAccount(AccountAuthenticatorResponse response,
                             String accountType, String authTokenType,
                             String[] requiredFeatures, Bundle options) {
        Intent intent = new Intent(mContext, LoginActivity.class);
        intent.setAction("com.bedroid.beEx.authsync.LOGIN");
        intent.putExtra(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE, response);
        Bundle bundle = new Bundle();
        bundle.putParcelable(AccountManager.KEY_INTENT, intent);
        return bundle;
    }

    @Override
    public Bundle confirmCredentials(AccountAuthenticatorResponse response,
                                     Account account, Bundle options) {
        return null;
    }

    @Override
    public Bundle editProperties(AccountAuthenticatorResponse response,
                                 String accountType) {
        return null;
    }
    @Override
    public Bundle getAuthToken(AccountAuthenticatorResponse response,
                               Account account, String authTokenType, Bundle loginOptions) {
        //return null;
        // Extract the username and password from the Account Manager, and ask
        // the server for an appropriate AuthToken.
        final AccountManager am = AccountManager.get(mContext);

        String authToken = am.peekAuthToken(account, authTokenType);

        // Lets give another try to authenticate the user
        if (TextUtils.isEmpty(authToken)) {
            final String password = am.getPassword(account);
            if (password != null) {
                /*ExchangeService service = new ExchangeService(ExchangeVersion.Exchange2010_SP2);
                ExchangeCredentials credentials = new WebCredentials(account.name, password);
                service.setCredentials(credentials);
                URI url = new URI(mServerUrl);
                service.setUrl(url);*/
                //authToken = sServerAuthenticate.userSignIn(account.name, password, authTokenType);
                //DO CONNECT TO SERVER HERE
            }
        }

        // If we get an authToken - we return it
        if (!TextUtils.isEmpty(authToken)) {
            final Bundle result = new Bundle();
            result.putString(AccountManager.KEY_ACCOUNT_NAME, account.name);
            result.putString(AccountManager.KEY_ACCOUNT_TYPE, account.type);
            result.putString(AccountManager.KEY_AUTHTOKEN, authToken);
            return result;
        }

        // If we get here, then we couldn't access the user's password - so we
        // need to re-prompt them for their credentials. We do that by creating
        // an intent to display our AuthenticatorActivity.
        final Intent intent = new Intent(mContext, LoginActivity.class);
        intent.putExtra(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE, response);
       //intent.putExtra(LoginActivity.ARG_ACCOUNT_TYPE, account.type);
        final Bundle bundle = new Bundle();
        bundle.putParcelable(AccountManager.KEY_INTENT, intent);
        return bundle;
    }

    @Override
    public String getAuthTokenLabel(String authTokenType) {
        return null;
    }

    @Override
    public Bundle hasFeatures(AccountAuthenticatorResponse response,
                              Account account, String[] features) {
        return null;
    }

    @Override
    public Bundle updateCredentials(AccountAuthenticatorResponse response,
                                    Account account, String authTokenType, Bundle loginOptions) {
        return null;
    }
}