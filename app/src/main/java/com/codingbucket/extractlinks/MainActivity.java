package com.codingbucket.extractlinks;

import android.annotation.TargetApi;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;

import com.google.android.gms.vision.text.TextRecognizer;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.FileProvider;

import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.regex.Pattern;

public class MainActivity extends AppCompatActivity {

    private static final int PICK_IMAGE_REQUEST = 2;
    private static final String TAG = "CB";
    private static final String CHANNEL_ID = "32";
    private static final int NOTIFICATION_ID = 32;
    private ImageView imageView;
    private Uri photoURI;
    private TextRecognizer textRecognizer=null;
    private static final String COLON_SEPARATOR = ":";
    private static final String IMAGE = "image";
    ListView devicelistLinks,devicelistText;
    private static  Pattern pattern=Pattern.compile("^(http:\\/\\/www\\.|https:\\/\\/www\\.|http:\\/\\/|https:\\/\\/)?[a-z0-9]+([\\-\\.]{1}[a-z0-9]+)*\\.[a-z]{2,5}(:[0-9]{1,5})?(\\/.*)?$");
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        textRecognizer = new TextRecognizer.Builder(getApplicationContext()).build();
        imageView = new ImageView(this);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab2 = findViewById(R.id.fab2);
        fab2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dispatchTakePictureIntent();
            }
        });
        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Adding Notification", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
                addNotificaiton();
            }
        });
        devicelistLinks = (ListView) findViewById(R.id.listView);
        devicelistText = (ListView) findViewById(R.id.listView2);

        try {
            if (getIntent().getExtras().getBoolean("FROM_NOTIFICATION")) {
                dispatchTakePictureIntent();
            }
        } catch (Exception e) {

        }
    }
    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        // Ensure that there's a camera activity to handle the intent
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            // Create the File where the photo should go
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                // Error occurred while creating the File
                showMessage("Faied to create file");
            }
            // Continue only if the File was successfully created
            if (photoFile != null) {
                Uri photoURI = FileProvider.getUriForFile(this,
                        "com.codingbucket.android.fileprovider",
                        photoFile);
                this.photoURI = photoURI;
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                startActivityForResult(takePictureIntent, REQUEST_TAKE_PHOTO);
            }
        }
    }


    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Bitmap bitmap = null;
        if (requestCode == REQUEST_TAKE_PHOTO && resultCode == RESULT_OK) {
            galleryAddPic();
            System.out.println(currentPhotoPath);
            try {
                bitmap = getBitMap(photoURI);
                System.out.println("Got bitmap " + bitmap.toString());
                List list = TextExtractor.getTexts(bitmap, textRecognizer);
                filterLinksAndSet(list);

            } catch (Exception e) {
                e.printStackTrace();
            }
            showMessage("Saved to device");

        }
    }

    private void addNotificaiton() {
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);

        createNotificationChannel();
        Intent myIntent = new Intent(MainActivity.this, MainActivity.class);
        myIntent.putExtra("FROM_NOTIFICATION", true);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, myIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("Tap to Extract text")
//                .setContentText()
                .setPriority(NotificationCompat.PRIORITY_MIN)
                .setContentIntent(pendingIntent)
                .setAutoCancel(false)
                .setOngoing(true);
        notificationManager.notify(NOTIFICATION_ID, builder.build());

    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "notificaiton";
            String description = "des";
            int importance = NotificationManager.IMPORTANCE_NONE;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);
            channel.setShowBadge(false);
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    static final int REQUEST_TAKE_PHOTO = 1;

    public void chooseImage() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Select Picture"), PICK_IMAGE_REQUEST);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }


    @RequiresApi(api = Build.VERSION_CODES.N)
    private void filterLinksAndSet(List list) {
        List<String> links=new ArrayList<>();
        List<String> texts=new ArrayList<>();
        list.stream().forEach(a->{
            if(String.valueOf(a).contains(".")){
                links.add(String.valueOf(a));
            }else{
                texts.add(String.valueOf(a));
            }
        });
        final ArrayAdapter adapter = new ArrayAdapter(this, android.R.layout.simple_list_item_1, links);
        devicelistLinks.setAdapter(adapter);
        devicelistLinks.setOnItemClickListener(myListClickListener); //Method called when the device from the list is clicked

        final ArrayAdapter adapter2 = new ArrayAdapter(this, android.R.layout.simple_list_item_1, texts);
        devicelistText.setAdapter(adapter2);
        devicelistText.setOnItemClickListener(myListClickListenerText); //Method called when the device from the list is clicked
        setClipboardText(getApplicationContext(), links.get(0));
        openLink(links.get(0));

    }

    String currentPhotoPath;

    private File createImageFile() throws IOException {
        // Create an image file name
        String imageFileName = "CODINGBUCKET_test";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );

        // Save a file: path for use with ACTION_VIEW intents
        currentPhotoPath = image.getAbsolutePath();
        return image;
    }

    private void galleryAddPic() {
        Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        File f = new File(currentPhotoPath);
        Uri contentUri = Uri.fromFile(f);
        mediaScanIntent.setData(contentUri);
        this.sendBroadcast(mediaScanIntent);
    }

    private void showMessage(String message) {
        Toast.makeText(this,
                message,
                Toast.LENGTH_SHORT).show();
    }

    public static Bitmap RotateBitmap(Bitmap source, int angle) {
        Matrix matrix = new Matrix();
        matrix.postRotate(angle);
        return Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(), matrix, true);
    }

    public Bitmap getBitMap(Uri uri) {
        try {
            Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), uri);
            // Log.d(TAG, String.valueOf(bitmap));
            System.out.println("Got bitmap for image");
            System.out.println();
            int rotation = getImageRotationDegrees(uri);
            Bitmap rotatedBitmap = RotateBitmap(bitmap, rotation);
            return rotatedBitmap;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public int getImageRotationDegrees(@NonNull Uri imgUri) {
        int photoRotation = ExifInterface.ORIENTATION_UNDEFINED;

        try {
            boolean hasRotation = false;
            //If image comes from the gallery and is not in the folder DCIM (Scheme: content://)
            String[] projection = {MediaStore.Images.ImageColumns.ORIENTATION};
            Cursor cursor = getApplicationContext().getContentResolver().query(imgUri, projection, null, null, null);
            if (cursor != null) {
                if (cursor.getColumnCount() > 0 && cursor.moveToFirst()) {
                    photoRotation = cursor.getInt(cursor.getColumnIndex(projection[0]));
                    hasRotation = photoRotation != 0;
                    Log.d(TAG, "Cursor orientation: " + photoRotation);
                }
                cursor.close();
            }

            //If image comes from the camera (Scheme: file://) or is from the folder DCIM (Scheme: content://)
            if (!hasRotation) {
                ExifInterface exif = new ExifInterface(currentPhotoPath);
                int exifRotation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION,
                        ExifInterface.ORIENTATION_NORMAL);
                switch (exifRotation) {
                    case ExifInterface.ORIENTATION_ROTATE_90: {
                        photoRotation = 90;
                        break;
                    }
                    case ExifInterface.ORIENTATION_ROTATE_180: {
                        photoRotation = 180;
                        break;
                    }
                    case ExifInterface.ORIENTATION_ROTATE_270: {
                        photoRotation = 270;
                        break;
                    }
                }
                Log.d(TAG, "Exif orientation: " + photoRotation);
            }
        } catch (IOException e) {
            Log.e(TAG, "Error determining rotation for image" + imgUri, e);
        }
        return photoRotation;
    }

    private AdapterView.OnItemClickListener myListClickListenerText = new AdapterView.OnItemClickListener() {
        public void onItemClick(AdapterView<?> av, View v, int arg2, long arg3) {
            // Get the device MAC address, the last 17 chars in the View
            String text = ((TextView) v).getText().toString();
            setClipboardText(getApplicationContext(), text);
        }
    };
    private AdapterView.OnItemClickListener myListClickListener = new AdapterView.OnItemClickListener() {
        public void onItemClick(AdapterView<?> av, View v, int arg2, long arg3) {
            // Get the device MAC address, the last 17 chars in the View
            String text = ((TextView) v).getText().toString();
            setClipboardText(getApplicationContext(), text);
            openLink(text);
//
        }
    };

    private void openLink(String text) {
        try {
            String[] splits = text.split(" ");
            for(String split:splits){
                try {
                    if (!split.contains("http")) {
                        split = "http://" + split;
                    }
                    if(pattern.matcher(split).find()) {
                        System.out.println(split);
                        new URL(split).toURI();
                        Intent i = new Intent(Intent.ACTION_VIEW);
                        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        i.setData(Uri.parse(split));
                        getApplication().startActivity(i);
                    }
                }catch (Exception e){
                    System.out.println("There was an error "+e.getMessage());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static boolean setClipboardText(Context context, String textToSet) {
        try {
            if (textToSet.equalsIgnoreCase("")) {
                Toast.makeText(context, "Nothing to set", Toast.LENGTH_SHORT).show();
            }
            ClipboardManager clipboard = (ClipboardManager) context.getSystemService(CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("From Linkshare", textToSet);
            clipboard.setPrimaryClip(clip);
            Toast.makeText(context, "Copied " + textToSet, Toast.LENGTH_SHORT).show();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }
}
