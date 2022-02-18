package de.androidcrypto.sendemailintentattachment;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static androidx.core.content.FileProvider.getUriForFile;

public class MainActivity extends AppCompatActivity {

    // funktioniert internal + external auf real device

    Button btnSendEmailInternal, btnSendEmailExternal;

    String dataToStore = "some data";
    byte[] data = dataToStore.getBytes(StandardCharsets.UTF_8);
    String emailAddress = "androidcrypto@gmx.de";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ActivityResultLauncher<Intent> saveActivityResultLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                new ActivityResultCallback<ActivityResult>() {
                    @Override
                    public void onActivityResult(ActivityResult result) {
                        if (result.getResultCode() == Activity.RESULT_OK) {
                            // There are no request codes
                            Intent data = result.getData();
                            if (data != null
                                    && data.getData() != null) {
                                writeFileToExternalStorage(data.getData(), dataToStore);
                                //uriExternal = data.getData();
                                sendEmailWithAttachment(data.getData());
                            }
                        }
                    }
                });

        btnSendEmailInternal = (Button) findViewById(R.id.btnSendEmailInternal);
        btnSendEmailInternal.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // save some data to file in internal storage

                String filename = "testInternal.txt";
                String subfolder = "tdat";

                boolean writeSuccess = writeFileToInternalStorage(filename, subfolder, data);
                System.out.println("writeSuccess: " + writeSuccess);
                if (writeSuccess) {
                    File filePath = new File(getFilesDir(), subfolder);
                    File fullFile = new File(filePath, filename);
                    Context context = getApplicationContext();
                    Uri contentUri = getUriForFile(context, "de.androidcrypto.sendemailintentattachment.provider", fullFile);
                    System.out.println("contentUri: " + contentUri);

                    // build the emailIntent
                    try {
                        //String email = "test@test.com"; // change to a real email address you control
                        String email = emailAddress;
                        String subject = "email subject internal";
                        String message = "email message";
                        final Intent emailIntent = new Intent(android.content.Intent.ACTION_SEND);
                        emailIntent.setType("plain/text");
                        emailIntent.putExtra(android.content.Intent.EXTRA_EMAIL, new String[]{email});
                        emailIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, subject);
                        emailIntent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                        emailIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                        emailIntent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                        if (contentUri != null) {
                            System.out.println("contentUri is not null");
                            emailIntent.putExtra(Intent.EXTRA_STREAM, contentUri);
                        }
                        emailIntent.putExtra(android.content.Intent.EXTRA_TEXT, message);
                        System.out.println("before MainActivity.this.startActivity");
                        if (emailIntent.resolveActivity(getPackageManager()) != null) {
                            //startActivity(intent);
                            MainActivity.this.startActivity(Intent.createChooser(emailIntent, "Sending email..."));
                        }

                        System.out.println("after MainActivity.this.startActivity");
                    } catch (SecurityException e) {
                        System.out.println("error: " + e.toString());
                        Toast.makeText(MainActivity.this, "Request failed try again: " + e.toString(), Toast.LENGTH_LONG).show();
                    }
                }
            }
        });

        btnSendEmailExternal = (Button) findViewById(R.id.btnSendEmailExternal);
        btnSendEmailExternal.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String filename = "testExternal"; // .txt is appended

                try {
                    Intent saveTextFileIntent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
                    saveTextFileIntent.addCategory(Intent.CATEGORY_OPENABLE);
                    saveTextFileIntent.setType("text/plain");
                    saveTextFileIntent.putExtra(
                            Intent.EXTRA_TITLE,
                            filename + ".txt"
                    );
                    saveActivityResultLauncher.launch(saveTextFileIntent);
                    //MainActivity.this.startActivity(saveTextFileIntent);
                    //startActivityForResult(saveTextFileIntent, CREATE_FILE);
                    //startActivityForResult(saveTextFileIntent, SettingsHelper.FILE_CREATE);
                } catch (ActivityNotFoundException e) {
                    System.out.println("activityNotFound: " + e.toString());
                }
            }
        });
    }

    private boolean writeFileToInternalStorage(String filename, String path, byte[] data) {
        try {
            File dir=new File(getFilesDir(), path);
            if (!dir.exists()) {
                dir.mkdirs();
            }
            System.out.println("** dir: " + dir.toString());
            File newFile = new File(dir, filename);
            System.out.println("newFile: " + newFile.toString());
            FileOutputStream output = new FileOutputStream(new File(dir, filename));
            ByteArrayInputStream input = new ByteArrayInputStream(data);
            int DEFAULT_BUFFER_SIZE = 1024;
            byte[] buffer = new byte[DEFAULT_BUFFER_SIZE];
            int n = 0;
            n = input.read(buffer, 0, DEFAULT_BUFFER_SIZE);
            while (n >= 0) {
                output.write(buffer, 0, n);
                n = input.read(buffer, 0, DEFAULT_BUFFER_SIZE);
            }
            output.close();
            input.close();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private void writeFileToExternalStorage(@NonNull Uri uri, @NonNull String text) {
        OutputStream outputStream;
        try {
            outputStream = getContentResolver().openOutputStream(uri);
            BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(outputStream));
            bw.write(text);
            bw.flush();
            bw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void sendEmailWithAttachment(@NonNull Uri uri) {
        // load file data
        try {
            String email = emailAddress;
            String subject = "email subject external";
            String message = "email message";
            final Intent emailIntent = new Intent(android.content.Intent.ACTION_SEND);
            emailIntent.setType("plain/text");
            emailIntent.putExtra(android.content.Intent.EXTRA_EMAIL, new String[]{email});
            emailIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, subject);
            if (uri != null) {
                emailIntent.putExtra(Intent.EXTRA_STREAM, uri);
            }
            emailIntent.putExtra(android.content.Intent.EXTRA_TEXT, message);
            if (emailIntent.resolveActivity(getPackageManager()) != null) {
                MainActivity.this.startActivity(Intent.createChooser(emailIntent, "Sending email..."));
            }
        } catch (Exception e) {
            Toast.makeText(MainActivity.this, "Request failed try again: " + e.toString(), Toast.LENGTH_LONG).show();
        }
    }
}