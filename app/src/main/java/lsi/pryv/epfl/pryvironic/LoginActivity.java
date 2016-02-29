package lsi.pryv.epfl.pryvironic;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.EditText;

import com.pryv.Pryv;
import com.pryv.api.model.Permission;
import com.pryv.auth.AuthController;
import com.pryv.auth.AuthControllerImpl;
import com.pryv.auth.AuthView;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class LoginActivity extends AppCompatActivity {
    private EditText passwordField;
    private EditText userField;

    private String user = "";
    private String tk = "";

    private String reqAppId = "epfl-lsi-ironic";
    private List<Permission> permissions = new ArrayList<Permission>();
    private String streamId1 = "pics";
    private Permission.Level perm1 = Permission.Level.contribute;
    private String defaultName1 = "ddd";
    private Permission testPermission1 = new Permission(streamId1, perm1, defaultName1);
    private String streamId2 = "vids";
    private Permission.Level perm2 = Permission.Level.read;
    private String defaultName2 = "eee";
    private Permission testPermission2 = new Permission(streamId2, perm2, defaultName2);
    private String lang = "en";
    private String returnURL = "fakeURL";

    private String webViewUrl;
    private String message;

    private WebView web;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        web = (WebView) findViewById(R.id.webview);

        permissions.add(testPermission1);
        permissions.add(testPermission2);
        Pryv.setDomain("pryv-switch.ch");
        new SigninAsync().execute();
    }

    private class SigninAsync extends AsyncTask<Void, Void, Void> {

        private ProgressDialog progressDialog;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            progressDialog = new ProgressDialog(LoginActivity.this);
            progressDialog.setMessage("Signin...");
            progressDialog.show();
            progressDialog.setCancelable(false);
        }

        @Override
        protected Void doInBackground(Void... arg0) {
            AuthController authenticator = new AuthControllerImpl(reqAppId, permissions, lang, returnURL, new CustomAuthView());
            authenticator.signIn();
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            if(webViewUrl!=null) {
                progressDialog.dismiss();
                web.loadUrl(webViewUrl);
                web.requestFocus(View.FOCUS_DOWN);
                web.getSettings().setJavaScriptEnabled(true);
                web.getSettings().setUseWideViewPort(true);
            } else {
                progressDialog.setMessage(message);
            }
        }
    }

    private class CustomAuthView implements AuthView {

        @Override
        public void displayLoginView(String loginURL) {
            webViewUrl = loginURL;
        }

        @Override
        public void onAuthSuccess(String username, String token) {
            CreditentialsManager.setCreditentials(username, token);
            Intent intent = new Intent(LoginActivity.this, MainActivity.class);
        }

        @Override
        public void onAuthError(String msg) {
            message = msg;
        }

        @Override
        public void onAuthRefused(int reasonId, String msg, String detail) {
            message = msg;
        }

    }

}