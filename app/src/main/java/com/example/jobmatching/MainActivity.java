package com.example.jobmatching;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.view.View;
import android.widget.Adapter;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.RequestFuture;
import com.android.volley.toolbox.Volley;
import com.androidnetworking.AndroidNetworking;
import com.androidnetworking.common.Priority;
import com.androidnetworking.error.ANError;
import com.androidnetworking.interfaces.JSONArrayRequestListener;
import com.androidnetworking.interfaces.JSONObjectRequestListener;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.security.AllPermission;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class MainActivity extends AppCompatActivity {

    private Button fetch_jobs_button;

    private ListView skills_list_view;

    private RequestQueue jobs_queue;
    private RequestQueue skills_queue;

    private ArrayList<String> skills_array;

    private EditText percentage_box;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        AndroidNetworking.initialize(getApplicationContext());

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Get GUI elements
        fetch_jobs_button = findViewById(R.id.fetch_jobs);
        skills_list_view = findViewById(R.id.skills_list_view);
        percentage_box = findViewById(R.id.percentage);

        // Create request objects
        jobs_queue = Volley.newRequestQueue(this);
        skills_queue = Volley.newRequestQueue(this);

        // Skills got from REST API
        skills_array = new ArrayList<String>();

        // Fill ListView with the skills got from the API when the Activity is created
        get_skills_from_api();

        // Set request objects
        fetch_jobs_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!validate_percentage()) return;
                int percentage_value = Integer.parseInt(percentage_box.getText().toString());
                HashMap<String, Integer> jobs = get_jobs_from_api(percentage_value, get_selected_skills());
                Log.d("TestJFAReturn", "onClick: " + jobs);
            }
        });
    }


    private ArrayList<String> get_selected_skills() {
        // Skills selected from ListView as array
        ArrayList<String> skills_selected = new ArrayList<>();

        // Selected skills from ListView
        SparseBooleanArray selected_skills_from_list = skills_list_view.getCheckedItemPositions();

        // Iterate over the ListView elements, checks which ones are checked and add the checked ones
        // to the array list
        for (int i=0; i < skills_list_view.getCount(); i++) {
            if (skills_list_view.isItemChecked(i)) {
                skills_selected.add(skills_list_view.getItemAtPosition(i).toString());
            }
        }

        return skills_selected;
    }

    private boolean validate_percentage() {
        int percentage_value;
        // Check if percentage is set
        if (TextUtils.isEmpty(percentage_box.getText())) {
            percentage_box.setError("This field is required");
            return false;
        } else {
            percentage_value = Integer.parseInt(percentage_box.getText().toString());
        }

        if (percentage_value > 100) {
            percentage_box.setError("Percentage can't be higher than 100");
            return false;
        }

        return true;
    }


    public void get_skills_from_api() {
        JSONObject jsonObject = new JSONObject();

        AndroidNetworking.post("http://192.168.100.65:8000/skills")
                .addJSONObjectBody(jsonObject) // posting json
                .setTag("test")
                .setPriority(Priority.MEDIUM)
                .build()
                .getAsJSONObject(new JSONObjectRequestListener() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            // Makes it list item a "checkbox"
                            ArrayAdapter<String> skills_adapter = new ArrayAdapter<String>(
                                    getApplicationContext(),
                                    android.R.layout.simple_list_item_multiple_choice, skills_array);

                            skills_list_view.setAdapter(skills_adapter);
                            skills_list_view.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);


                            JSONArray skills = response.getJSONArray("result");
                            for (int i=0; i < skills.length(); i++) {
                                skills_array.add(skills.getString(i));
                            }

                            Log.d("FAN", "onResponse: " + skills_array.toString());

                            Toast.makeText(getApplicationContext(),
                                    "Skills got successfully",
                                    Toast.LENGTH_LONG).show();
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }

                    @Override
                    public void onError(ANError error) {
                        // handle error
                        Toast.makeText(getApplicationContext(),
                                "Couldn't get skills from the server",
                                Toast.LENGTH_LONG).show();
                        error.printStackTrace();
                    }
                });
    }


    private HashMap<String, Integer> get_jobs_from_api(int percentage, ArrayList<String> skills_selected) {
        String url = "http://192.168.100.65:8000/job_matching";
        JSONArray skills = new JSONArray(skills_selected);
        JSONObject json_body = new JSONObject();

        // To store the return value: jobs and its matching percentages
        final HashMap<String, Integer> jobs_and_percentages = new HashMap<>();

        // Creates request body
        try {
            json_body.put("match_percentage", percentage);
            json_body.put("skills", skills);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        AndroidNetworking.post("http://192.168.100.65:8000/job_matching")
                .addJSONObjectBody(json_body) // posting json
                .setTag("test")
                .setPriority(Priority.MEDIUM)
                .build()
                .getAsJSONObject(new JSONObjectRequestListener() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            String job_value;
                            int job_percentage_value;

                            JSONArray all_jobs_percentages = response.getJSONArray("result");
                            for (int i=0; i < all_jobs_percentages.length(); i++) {
                                JSONArray job_percentage = all_jobs_percentages.getJSONArray(i);
                                job_value = job_percentage.getString(0);
                                job_percentage_value = job_percentage.getInt(1);
                                jobs_and_percentages.put(job_value, job_percentage_value);
                            }

                            Toast.makeText(getApplicationContext(),
                                    "Matching jobs got successfully",
                                    Toast.LENGTH_LONG).show();
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }

                    @Override
                    public void onError(ANError error) {
                        // handle error
                        Toast.makeText(getApplicationContext(),
                                "Couldn't get matching jobs from the server",
                                Toast.LENGTH_LONG).show();
                        error.printStackTrace();
                    }
                });

        return jobs_and_percentages;
    }
}