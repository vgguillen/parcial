package com.example.parcial;


import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.vision.v1.Vision;
import com.google.api.services.vision.v1.VisionRequestInitializer;
import com.google.api.services.vision.v1.model.AnnotateImageRequest;
import com.google.api.services.vision.v1.model.AnnotateImageResponse;
import com.google.api.services.vision.v1.model.BatchAnnotateImagesRequest;
import com.google.api.services.vision.v1.model.BatchAnnotateImagesResponse;
import com.google.api.services.vision.v1.model.EntityAnnotation;
import com.google.api.services.vision.v1.model.Feature;
import com.google.api.services.vision.v1.model.Image;
import com.google.api.services.vision.v1.model.LocationInfo;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class RecognizeGallery extends AppCompatActivity {

    private Bitmap imgBitmap;
    private ImageView imageView;
    private TextView textView;
    private TextView pais, info;
    private Bundle bundle;

    // Cloud Vision Api KEY
    private Feature feature;
    private static final String CLOUD_VISION_API_KEY = "AIzaSyB5MkIB5lNnQH1kC1tZ3ATeEsv7z66moKs";
    private RequestQueue request;
    private StringRequest stringRequest;
    private JSONObject countriesObject;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recognize_gallery);

        imageView = findViewById(R.id.ivphoto);
        textView = findViewById(R.id.txtdisplay);
        pais  = findViewById(R.id.txtNombrePais);
        info = findViewById(R.id.txtInfoPais);



        textView.setMovementMethod(new ScrollingMovementMethod());
        bundle = getIntent().getExtras();
        feature = new Feature();
        feature.setType("LANDMARK_DETECTION");
        feature.setMaxResults(15);
        captureImage();
        getCountryData();

    }
    private void getCountryData() {
        request = Volley.newRequestQueue(RecognizeGallery.this);
        String URL = "http://www.geognos.com/api/en/countries/info/all.json";
        stringRequest = new StringRequest(Request.Method.GET, URL, response -> {
            countriesObject = null;
            try {
                countriesObject = new JSONObject(response);
                if (countriesObject.getJSONObject("Results") == null) {
                    Toast.makeText(RecognizeGallery.this, "No hay información", Toast.LENGTH_LONG).show();
                    return;
                } else {
                    countriesObject = countriesObject.getJSONObject("Results");
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Toast.makeText(RecognizeGallery.this, "Error:" + error.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
        request.add(stringRequest);
    }

    private void captureImage() {
        Uri myUri = Uri.parse(bundle.getString("imageUri"));
        imageView.setImageResource(android.R.color.transparent);
        imgBitmap = null;
        try
        {
            imgBitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), myUri);
        }
        catch
        (IOException e)
        {
            Log.e("ERROR", "Error de conversión de archivo: " + e.getMessage());
        }
        imageView.setImageBitmap(imgBitmap);

    }



    public void pressback(View view)
    {
        finish();
    }


    private Image getImageEncode(Bitmap bitmap) {
        Image base64EncodedImage = new Image();
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, byteArrayOutputStream);
        byte[] imageBytes = byteArrayOutputStream.toByteArray();
        base64EncodedImage.encodeContent(imageBytes);
        return base64EncodedImage;
    }
    private String formatAnnotation(List<EntityAnnotation> entityAnnotation) {
        String msg = "";
        if (entityAnnotation != null) {
            for (EntityAnnotation entity : entityAnnotation) {
                LocationInfo info = entity.getLocations().listIterator().next();
                msg = info.getLatLng().getLatitude() + " " +  info.getLatLng().getLongitude();
                msg += ":";
            }
        } else {
            msg = "";
        }
        return msg;
    }
    private String convertResponseToString(BatchAnnotateImagesResponse response) {
        AnnotateImageResponse imageResponses = response.getResponses().get(0);
        List<EntityAnnotation> entityAnnotations;
        String msg = "";
        // LANDMARK_DETECTION
        entityAnnotations = imageResponses.getLandmarkAnnotations();
        msg = formatAnnotation(entityAnnotations);
        return msg;
    }





    private String searchCountryCode(String points) throws JSONException {
        String[] pointsArray = points.split(":");
        for (String point : pointsArray) {
            String[] latlng = point.split(" ");
            Iterator<String> temp = countriesObject.keys();
            while (temp.hasNext()) {
                String key = temp.next();
                JSONObject country = countriesObject.getJSONObject(key);
                JSONObject geoRect = country.getJSONObject("GeoRectangle");
                if (Double.valueOf(latlng[0]) <= geoRect.getDouble("North") &&
                        Double.valueOf(latlng[0]) >= geoRect.getDouble("South") &&
                        Double.valueOf(latlng[1]) <= geoRect.getDouble("East") &&
                        Double.valueOf(latlng[1]) >= geoRect.getDouble("West")) {
                    return country.getJSONObject("CountryCodes").getString("iso2");
                }
            }
        }
        return "";
    }

}