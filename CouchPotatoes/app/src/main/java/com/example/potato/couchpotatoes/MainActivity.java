package com.example.potato.couchpotatoes;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.StrictMode;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;

public class MainActivity extends AppCompatActivity {
    private DBHelper helper;
    private android.widget.TextView userName;
    private android.widget.Button logout;
    private android.widget.Button chat;

    private Uri imageCaptureUri;
    private ImageView mImageView;
    private Button uploadImage;
    private static final int PICK_FROM_CAMERA = 1;
    private static final int PICK_FROM_FILE = 2;
    private InputStream is, is2;
    private String userID, photoID;
    private DialogInterface.OnClickListener dialogClickListener;

    // Image chooser source: https://www.youtube.com/watch?v=UiqmekHYCSU

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Source: https://stackoverflow.com/questions/42251634/android-os-fileuriexposedexception-file-jpg-exposed-beyond-app-through-clipdata
        StrictMode.VmPolicy.Builder builder2 = new StrictMode.VmPolicy.Builder();
        StrictMode.setVmPolicy(builder2.build());

        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        final String[] items = new String[] { "From Camera", "From SD Card" };
        ArrayAdapter<String> adapter = new ArrayAdapter<String>( this, android.R.layout.select_dialog_item, items );
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Select Image");
        builder.setAdapter(adapter, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if ( which == 0 ) {
                    Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE );
                    File file = new File(Environment.getExternalStorageDirectory(), "snapshot" + String.valueOf( System.currentTimeMillis() + ".jpg"));

                    imageCaptureUri = Uri.fromFile( file );

                    try {
                        intent.putExtra(MediaStore.EXTRA_OUTPUT, imageCaptureUri );
                        intent.putExtra("return data", true );

                        startActivityForResult( intent, PICK_FROM_CAMERA);
                    } catch ( Exception e ) {
                        e.printStackTrace();
                    }

                    dialog.cancel();
                }
                else {
                    Intent intent = new Intent();
                    intent.setType( "image/*" );
                    intent.setAction( Intent.ACTION_GET_CONTENT );
                    startActivityForResult( Intent.createChooser(intent, "Complete action using"), PICK_FROM_FILE );
                }
            }
        });

        final AlertDialog dialog = builder.create();

        mImageView = (ImageView) findViewById( R.id.testImageView );
        uploadImage = (Button) findViewById( R.id.uploadImage);
        uploadImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.show();
            }
        });

        helper = new DBHelper();

        userName = (android.widget.TextView) findViewById(R.id.userName);
        logout = (android.widget.Button) findViewById(R.id.logout);
        chat = (android.widget.Button) findViewById(R.id.viewChats);

        // Display user's name if logged in
        if ( helper.isUserLoggedIn() ) {
            String displayName = helper.getAuthUserDisplayName();

            userName.setText( displayName );
        }
        // Else, redirect user to login page
        else {
            startActivity( new Intent( getApplicationContext(), LoginActivity.class ) );
            finish();
        }

        // Add event handler to logout button to begin user logout
        logout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                helper.getAuth().signOut();
                startActivity( new Intent( getApplicationContext(), LoginActivity.class ) );
                finish();
            }
        });

        // Add event handler to chat button to start the ChatRoomActivity
        chat.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent( getApplicationContext(), ChatRoomActivity.class );
                //intent.putExtra( "userName", userName.getText() );
                startActivity( intent );
            }
        });

        //helper.getStorage().getReference( "Photo/" + helper.getAuth().getCurrentUser() );
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if ( resultCode != RESULT_OK )
            return;

        Bitmap bitmap = null;
        String path = "";

        if ( requestCode == PICK_FROM_FILE) {
            imageCaptureUri = data.getData();
        }

        try {
            is = getContentResolver().openInputStream(imageCaptureUri);
            is2 = getContentResolver().openInputStream(imageCaptureUri);
        } catch ( FileNotFoundException e ) {
            e.printStackTrace();
        }
        if ( is != null ) {
            bitmap = BitmapFactory.decodeStream( is );
        }

        /*
        if ( requestCode == PICK_FROM_FILE) {
            imageCaptureUri = data.getData();

            //Log.d( "TEST", data.getDataString() );

            //path = getRealPathFromURI(imageCaptureUri);

            //File file = new File( getFilesDir(), imageCaptureUri.getLastPathSegment() );

            //String docID = DocumentsContract.getDocumentId( imageCaptureUri );
            //String[] split = docID.split( ":" );
            //String type = split[0];

            try {
                is = getContentResolver().openInputStream(imageCaptureUri);
            } catch ( FileNotFoundException e ) {
                e.printStackTrace();
            }

            //Log.d( "TESTT", Environment.getExternalStorageDirectory().toString() );


            //if ( path == null ) {
            //    path = imageCaptureUri.getPath();
            //}
            //if ( path != null ) {
              //  bitmap = BitmapFactory.decodeFile( path );
                //bitmap = BitmapFactory.decodeFile( path );

            //}
            //Log.d( "TEST", imageCaptureUri.getEncodedPath() );

            if ( is != null ) {
                bitmap = BitmapFactory.decodeStream( is );
            }
        }
        else {
            //path = imageCaptureUri.getPath();
            //bitmap = BitmapFactory.decodeFile( path );
            //Log.d( "TEST", path );
            try {
                is = getContentResolver().openInputStream(imageCaptureUri);
            } catch ( FileNotFoundException e ) {
                e.printStackTrace();
            }
            if ( is != null ) {
                bitmap = BitmapFactory.decodeStream( is );
            }
        }
        */

        mImageView.setImageBitmap( bitmap );

        dialogClickListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which){
                    // Case to delete the clicked message
                    case DialogInterface.BUTTON_POSITIVE:
                        //Yes button clicked
                        Log.d( "TEST", "YES" );
                        UploadImage( is2 );
                        break;
                    // Case to cancel delete operation
                    case DialogInterface.BUTTON_NEGATIVE:
                        // Do nothing. User does not want to delete the clicked message
                        Log.d( "TEST", "NO" );
                        break;
                }
            }
        };

        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setMessage( "Upload image?" )
                .setNegativeButton("No", dialogClickListener)
                .setPositiveButton("Yes", dialogClickListener)
                //.setNeutralButton("Cancel", dialogClickListener2)
                .show();
    }

    public String getRealPathFromURI ( Uri uri ) {
        String[] proj = { MediaStore.Images.Media.DATA };
        Cursor cursor = managedQuery(uri, proj, null, null, null );
        if ( cursor == null ) return null;
        int column_index = cursor.getColumnIndexOrThrow( MediaStore.Images.Media.DATA );
        cursor.moveToFirst();
        return cursor.getString( column_index );
    }

    public void UploadImage ( InputStream is2 ) {
        // Upload image
        photoID = helper.getNewChildKey( helper.getPhotoPath() );
        userID = helper.getAuth().getUid();

        StorageReference ref = helper.getStorage().getReference().child( helper.getPhotoPath() + userID + "/" + photoID );

        UploadTask uploadTask = ref.putStream( is2 );

        uploadTask.addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Log.d( "TEST", "File uplaod failed" );
            }
        }).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                Log.d( "TEST", "File upload success" );

                // Get photo uri from Firebase Storage
                Uri downloadUrl = taskSnapshot.getDownloadUrl();

                Log.d( "TEST", "Download URL: " + downloadUrl );

                String title = imageCaptureUri.getLastPathSegment();
                String descr = "";
                String uri = ( downloadUrl != null ) ? downloadUrl.toString() : "";

                // Add photo meta data to Firebase Database
                helper.addToUserPhoto( userID, photoID );
                helper.addToPhoto( photoID, userID, title, descr, uri );
            }
        });
    }
}
