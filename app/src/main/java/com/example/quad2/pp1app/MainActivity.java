package com.example.quad2.pp1app;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.digits.sdk.android.AuthCallback;
import com.digits.sdk.android.DigitsAuthButton;
import com.digits.sdk.android.DigitsException;
import com.digits.sdk.android.DigitsSession;
import com.facebook.accountkit.AccessToken;
import com.facebook.accountkit.AccountKit;
import com.facebook.accountkit.AccountKitLoginResult;
import com.facebook.accountkit.ui.AccountKitActivity;
import com.facebook.accountkit.ui.AccountKitConfiguration;
import com.facebook.accountkit.ui.LoginType;

import java.util.HashMap;
import java.util.Map;

import io.branch.indexing.BranchUniversalObject;
import io.branch.referral.Branch;
import io.branch.referral.BranchError;
import io.branch.referral.SharingHelper;
import io.branch.referral.util.LinkProperties;
import io.branch.referral.util.ShareSheetStyle;

public class MainActivity extends AppCompatActivity {

    private DigitsAuthButton digitsButton;
    private BranchUniversalObject branchUniversalObject;

    private static final int FRAMEWORK_REQUEST_CODE = 1;

    private int nextPermissionsRequestCode = 4000;
    private final Map<Integer, OnCompleteListener> permissionsListeners = new HashMap<>();

    private interface OnCompleteListener {
        void onComplete();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        registerCallBackDigits();

        findViewById(R.id.fb_phone_login).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onLogin(LoginType.PHONE);
            }
        });
        findViewById(R.id.fb_email_login).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onLogin(LoginType.EMAIL);
            }
        });
        findViewById(R.id.buttonShare).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                initiateSharing();
            }
        });
        contentLoaded();

        if (AccountKit.getCurrentAccessToken() != null) {
            startActivity(new Intent(this, TokenActivity.class));
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        Branch branch = Branch.getInstance();
        branch.initSession(new Branch.BranchUniversalReferralInitListener() {
            @Override
            public void onInitFinished(BranchUniversalObject branchUniversalObject, LinkProperties linkProperties, BranchError error) {
                if (error == null && branchUniversalObject != null) {
                    // This code will execute when your app is opened from a Branch deep link, which
                    // means that you can route to a custom activity depending on what they clicked.
                    // In this example, we'll just print out the data from the link that was clicked.

                    Log.i("BranchTestBed", "title " + branchUniversalObject.getTitle());
                    Log.i("ContentMetaData", "metadata " + branchUniversalObject.getMetadata());
                }
            }
        }, this.getIntent().getData(), this);
    }


    @Override
    protected void onActivityResult(
            final int requestCode,
            final int resultCode,
            final Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode != FRAMEWORK_REQUEST_CODE) {
            return;
        }

        final String toastMessage;
        final AccountKitLoginResult loginResult = AccountKit.loginResultWithIntent(data);
        if (loginResult == null || loginResult.wasCancelled()) {
            toastMessage = "Login Cancelled";
        } else if (loginResult.getError() != null) {
            toastMessage = loginResult.getError().getErrorType().getMessage();
            final Intent intent = new Intent(this, ErrorActivity.class);
            intent.putExtra(ErrorActivity.HELLO_TOKEN_ACTIVITY_ERROR_EXTRA, loginResult.getError());

            startActivity(intent);
        } else {
            final AccessToken accessToken = loginResult.getAccessToken();
            final long tokenRefreshIntervalInSeconds =
                    loginResult.getTokenRefreshIntervalInSeconds();
            if (accessToken != null) {
                toastMessage = "Success:" + accessToken.getAccountId()
                        + tokenRefreshIntervalInSeconds;
                startActivity(new Intent(this, TokenActivity.class));
            } else {
                toastMessage = "Unknown response type";
            }
        }

        Toast.makeText(this, toastMessage, Toast.LENGTH_LONG).show();
    }

    private void onLogin(final LoginType loginType) {
        final Intent intent = new Intent(this, AccountKitActivity.class);
        final AccountKitConfiguration.AccountKitConfigurationBuilder configurationBuilder
                = new AccountKitConfiguration.AccountKitConfigurationBuilder(
                loginType,
                AccountKitActivity.ResponseType.TOKEN);
        final AccountKitConfiguration configuration = configurationBuilder.build();
        intent.putExtra(
                AccountKitActivity.ACCOUNT_KIT_ACTIVITY_CONFIGURATION,
                configuration);
        OnCompleteListener completeListener = new OnCompleteListener() {
            @Override
            public void onComplete() {
                startActivityForResult(intent, FRAMEWORK_REQUEST_CODE);
            }
        };
        switch (loginType) {
            case EMAIL:
                final OnCompleteListener getAccountsCompleteListener = completeListener;
                completeListener = new OnCompleteListener() {
                    @Override
                    public void onComplete() {
                        requestPermissions(
                                Manifest.permission.GET_ACCOUNTS,
                                R.string.permissions_get_accounts_title,
                                R.string.permissions_get_accounts_message,
                                getAccountsCompleteListener);
                    }
                };
                break;
            case PHONE:
                if (configuration.isReceiveSMSEnabled()) {
                    final OnCompleteListener receiveSMSCompleteListener = completeListener;
                    completeListener = new OnCompleteListener() {
                        @Override
                        public void onComplete() {
                            requestPermissions(
                                    Manifest.permission.RECEIVE_SMS,
                                    R.string.permissions_receive_sms_title,
                                    R.string.permissions_receive_sms_message,
                                    receiveSMSCompleteListener);
                        }
                    };
                }
                if (configuration.isReadPhoneStateEnabled()) {
                    final OnCompleteListener readPhoneStateCompleteListener = completeListener;
                    completeListener = new OnCompleteListener() {
                        @Override
                        public void onComplete() {
                            requestPermissions(
                                    Manifest.permission.READ_PHONE_STATE,
                                    R.string.permissions_read_phone_state_title,
                                    R.string.permissions_read_phone_state_message,
                                    readPhoneStateCompleteListener);
                        }
                    };
                }
                break;
        }
        completeListener.onComplete();
    }

    private void requestPermissions(
            final String permission,
            final int rationaleTitleResourceId,
            final int rationaleMessageResourceId,
            final OnCompleteListener listener) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            if (listener != null) {
                listener.onComplete();
            }
            return;
        }

        checkRequestPermissions(
                permission,
                rationaleTitleResourceId,
                rationaleMessageResourceId,
                listener);
    }

    @TargetApi(23)
    private void checkRequestPermissions(
            final String permission,
            final int rationaleTitleResourceId,
            final int rationaleMessageResourceId,
            final OnCompleteListener listener) {
        if (checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED) {
            if (listener != null) {
                listener.onComplete();
            }
            return;
        }

        final int requestCode = nextPermissionsRequestCode++;
        permissionsListeners.put(requestCode, listener);

        if (shouldShowRequestPermissionRationale(permission)) {
            new AlertDialog.Builder(this)
                    .setTitle(rationaleTitleResourceId)
                    .setMessage(rationaleMessageResourceId)
                    .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(final DialogInterface dialog, final int which) {
                            requestPermissions(new String[]{permission}, requestCode);
                        }
                    })
                    .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(final DialogInterface dialog, final int which) {
                            // ignore and clean up the listener
                            permissionsListeners.remove(requestCode);
                        }
                    })
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .show();
        } else {
            requestPermissions(new String[]{permission}, requestCode);
        }
    }

    @TargetApi(23)
    @SuppressWarnings("unused")
    @Override
    public void onRequestPermissionsResult(final int requestCode,
                                           final @NonNull String permissions[],
                                           final @NonNull int[] grantResults) {
        final OnCompleteListener permissionsListener = permissionsListeners.remove(requestCode);
        if (permissionsListener != null
                && grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            permissionsListener.onComplete();
        }
    }

    public void registerCallBackDigits() {
        digitsButton = (DigitsAuthButton) findViewById(R.id.auth_button);
        digitsButton.setCallback(new AuthCallback() {
            @Override
            public void success(DigitsSession session, String phoneNumber) {
                // TODO: associate the session userID with your user model
                Toast.makeText(getApplicationContext(), "Authentication successful for "
                        + phoneNumber, Toast.LENGTH_LONG).show();
            }

            @Override
            public void failure(DigitsException exception) {
                Log.d("Digits", "Sign in with Digits failure", exception);
            }
        });

    }

    // This would be your own method where you've loaded the content for this page
    void contentLoaded() {
        // Initialize a Branch Universal Object for the page the user is viewing
        branchUniversalObject = new BranchUniversalObject()
                .setCanonicalIdentifier("item_id_12345")
                .setTitle("My Content Title")
                .setContentDescription("Check out this awesome piece of content")
                .setContentImageUrl("https://example.com/mycontent-12345.png")
                .addContentMetadata("item_id", "12345")
                .addContentMetadata("user_id", "678910");

        // Trigger a view on the content for analytics tracking
        branchUniversalObject.registerView();

        // List on Google App Indexing
        branchUniversalObject.listOnGoogleSearch(this);
    }


    // This is the function to handle sharing when a user clicks the share button
    void initiateSharing() {
        // Create your link properties
        // More link properties available at https://dev.branch.io/getting-started/configuring-links/guide/#link-control-parameters
        final LinkProperties linkProperties = new LinkProperties()
                .setFeature("sharing");

        // Customize the appearance of your share sheet
        ShareSheetStyle shareSheetStyle = new ShareSheetStyle(this, "Check this out!", "Hey friend - I know you'll love this: ")
                .setCopyUrlStyle(getResources().getDrawable(android.R.drawable.ic_menu_send), "Copy link", "Link added to clipboard!")
                .setMoreOptionStyle(getResources().getDrawable(android.R.drawable.ic_menu_search), "Show more")
                .addPreferredSharingOption(SharingHelper.SHARE_WITH.FACEBOOK)
                .addPreferredSharingOption(SharingHelper.SHARE_WITH.EMAIL)
                .addPreferredSharingOption(SharingHelper.SHARE_WITH.MESSAGE)
                .addPreferredSharingOption(SharingHelper.SHARE_WITH.TWITTER);

        // Show the share sheet for the content you want the user to share. A link will be automatically created and put in the message.
        branchUniversalObject.showShareSheet(this, linkProperties, shareSheetStyle, new Branch.BranchLinkShareListener() {
            @Override
            public void onShareLinkDialogLaunched() {
            }

            @Override
            public void onShareLinkDialogDismissed() {
            }

            @Override
            public void onChannelSelected(String channelName) {
            }

            @Override
            public void onLinkShareResponse(String sharedLink, String sharedChannel, BranchError error) {
                // The link will be available in sharedLink
            }
        });
    }
}
