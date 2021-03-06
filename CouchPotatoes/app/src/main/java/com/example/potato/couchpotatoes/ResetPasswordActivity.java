package com.example.potato.couchpotatoes;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.MenuItem;
import android.view.View;
import android.support.v7.widget.Toolbar;

import android.widget.*;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;

public class ResetPasswordActivity extends AppCompatActivity {

    private EditText inputEmail;
    private ProgressBar progressBar;
    private ResetPasswordTask mResetTask = null;

    private DBHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        dbHelper = DBHelper.getInstance();

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reset_password);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        inputEmail = findViewById(R.id.reset_email);
        progressBar = findViewById(R.id.reset_progressBar);
        Button btnReset = findViewById(R.id.btn_reset_password);

        btnReset.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                attemptResetPassword();
            }
        });

        // Add back button in ActionBar
        android.support.v7.app.ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
    }

    /**
     * Function for ActionBar back button.
     */
    public boolean onOptionsItemSelected(MenuItem item){
        startActivity(new Intent(ResetPasswordActivity.this, LoginActivity.class));
        finish();
        return true;
    }
    /**
     * Overrides the back button from ending the activity.
     */
    public void onBackPressed(){
        startActivity(new Intent(ResetPasswordActivity.this, LoginActivity.class));
        finish();
    }

    private void attemptResetPassword() {
        if (mResetTask != null) {
            return;
        }

        // Reset errors.
        inputEmail.setError(null);

        // Store values at the time of the reset attempt.
        String email = inputEmail.getText().toString().trim();
        boolean cancel = false;
        View focusView = null;

        // Check for valid email address.
        if (TextUtils.isEmpty(email)) {
            inputEmail.setError("This field is required");
            focusView = inputEmail;
            cancel = true;
        } else if (!StringValidator.isValidEmail(email)) {
            inputEmail.setError("This email address is invalid");
            focusView = inputEmail;
            cancel = true;
        }

        if (cancel) {
            // There was an error; don't attempt login and focus the first
            // form field with an error.
            focusView.requestFocus();
        } else {
            // Show a progress spinner, and kick off a background task to
            // perform the user login attempt.
            progressBar.setVisibility(View.VISIBLE);
            mResetTask = new ResetPasswordTask(email);
            mResetTask.execute((Void) null);
        }
    }

    /**
     * Represents an asynchronous reset password task used to reset the password
     * of the user.
     */
    public class ResetPasswordTask extends AsyncTask<Void, Void, Boolean> {
        private final String mEmail;

        ResetPasswordTask(String email) {
            mEmail = email;
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            // Attempt to send the reset link with the passed email

            final boolean[] success = {false};
            dbHelper.getAuth().sendPasswordResetEmail(mEmail)
                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            if (task.isSuccessful()) {
                                AlertDialog.Builder builder = new AlertDialog.Builder(ResetPasswordActivity.this);
                                builder.setTitle("Password Reset Email Sent");
                                builder.setMessage(getText(R.string.reset_password_success_msg_1)
                                                + " " + mEmail + " "
                                                + getText(R.string.reset_password_success_msg_2));
                                builder.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                                            public void onClick(DialogInterface dialog, int which) {
                                                startActivity(new Intent(getApplicationContext(), LoginActivity.class));
                                                finish();
                                            }
                                        });
                                builder.show();
                                progressBar.setVisibility(View.GONE);
                                success[0] = true;
                            } else {
                                Toast.makeText(ResetPasswordActivity.this,
                                        "Failed to send reset email!",
                                        Toast.LENGTH_SHORT).show();
                            }

                            progressBar.setVisibility(View.GONE);
                        }
                    });

            return success[0];
        }

        @Override
        protected void onPostExecute(final Boolean success) {
            mResetTask = null;
        }

        @Override
        protected void onCancelled() {
            mResetTask = null;
            progressBar.setVisibility(View.VISIBLE);
        }
    }
}
