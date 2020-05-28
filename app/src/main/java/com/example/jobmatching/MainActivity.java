package com.example.jobmatching;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.SparseBooleanArray;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import com.android.volley.RequestQueue;
import com.android.volley.toolbox.Volley;
import com.androidnetworking.AndroidNetworking;
import com.androidnetworking.common.Priority;
import com.androidnetworking.error.ANError;
import com.androidnetworking.interfaces.JSONObjectRequestListener;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;

public class MainActivity extends AppCompatActivity {

    private Button fetch_jobs_button;

    private ListView skills_list_view;
    private ListView jobs_list_view;

    private RequestQueue jobs_queue;
    private RequestQueue skills_queue;

    private ArrayList<String> skills_array;
    private ArrayList<String> jobs_array;

    private EditText percentage_box;

    private final String base_url = "http://192.168.100.65:8000/";
    private final String skills_endpoint = "skills";
    private final String jobs_endpoint = "job_matching";

    // To store the return value: jobs and its matching percentages
    HashMap<String, Integer> jobs_and_percentages;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        AndroidNetworking.initialize(getApplicationContext());

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Get GUI elements
        fetch_jobs_button = findViewById(R.id.fetch_jobs);
        skills_list_view = findViewById(R.id.skills_list_view);
        jobs_list_view = findViewById(R.id.jobs_list_view);
        percentage_box = findViewById(R.id.percentage);

        // Create request objects
        jobs_queue = Volley.newRequestQueue(this);
        skills_queue = Volley.newRequestQueue(this);

        // Data got from REST API (Skills to select and matching jobs according to skills and
        // percentage selected
        skills_array = new ArrayList<String>();
        jobs_array = new ArrayList<String>();
        jobs_and_percentages = new HashMap<>();

        // Fill ListView with the skills got from the API when the Activity is created
        get_skills_from_api();

        // Set request objects
        fetch_jobs_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!validate_inputs()) return;
                int percentage_value = Integer.parseInt(percentage_box.getText().toString());
                ArrayList<String> selected_skills = get_selected_skills();
                get_jobs_from_api(percentage_value, selected_skills);
            }
        });
    }


    private ArrayList<String> get_selected_skills() {
        // Skills selected from ListView as array
        ArrayList<String> skills_selected = new ArrayList<>();

        // Iterate over the ListView elements, checks which ones are checked and add the checked ones
        // to the array list
        for (int i=0; i < skills_list_view.getCount(); i++) {
            if (skills_list_view.isItemChecked(i)) {
                skills_selected.add(skills_list_view.getItemAtPosition(i).toString());
            }
        }

        return skills_selected;
    }


    private boolean validate_inputs() {
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

        SparseBooleanArray selected_skills_from_list = skills_list_view.getCheckedItemPositions();

        if (selected_skills_from_list.size() < 1){
            Toast.makeText(getApplicationContext(), "Select at least 1 skill", Toast.LENGTH_LONG).show();
            return false;
        }


        return true;
    }


    public void get_skills_from_api() {
        JSONObject jsonObject = new JSONObject();

        AndroidNetworking.post(base_url+skills_endpoint)
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

                            Toast.makeText(getApplicationContext(),
                                    "Skills got successfully",
                                    Toast.LENGTH_SHORT).show();
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


    private void get_jobs_from_api(int percentage, ArrayList<String> skills_selected) {
        String url = base_url+skills_endpoint;
        JSONArray skills = new JSONArray(skills_selected);
        JSONObject json_body = new JSONObject();

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
                             // Makes it list item a "checkbox"
                             ArrayAdapter<String> jobs_adapter = new ArrayAdapter<String>(
                                     getApplicationContext(),
                                     android.R.layout.simple_list_item_1, jobs_array);

                             jobs_list_view.setAdapter(jobs_adapter);
                             jobs_list_view.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);

                            String job_value;
                            int job_percentage_value;

                            jobs_array.clear();

                            JSONArray all_jobs_percentages = response.getJSONArray("result");
                            for (int i=0; i < all_jobs_percentages.length(); i++) {
                                JSONArray job_percentage = all_jobs_percentages.getJSONArray(i);
                                job_value = job_percentage.getString(0);
                                job_percentage_value = job_percentage.getInt(1);
                                jobs_array.add(job_value + " (" + String.valueOf(job_percentage_value) + "%)");
                            }

                            if (jobs_array.size() < 1) {
                                jobs_array.add("No matches found.\nTry decreasing the match percentage or selecting more skills.");
                            } else {
                                Toast.makeText(getApplicationContext(),
                                        "Matching jobs got successfully",
                                        Toast.LENGTH_SHORT).show();
                            }

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
    }
}