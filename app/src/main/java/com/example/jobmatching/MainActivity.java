package com.example.jobmatching;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private TextView jobs_result;
    private TextView skills_result;
    private RequestQueue jobs_queue;
    private RequestQueue skills_queue;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Get GUI elements
        jobs_result = findViewById(R.id.show_jobs);
        skills_result = findViewById(R.id.show_skills);
        Button fetch_jobs_button = findViewById(R.id.fetch_jobs);
        Button fetch_skills_button = findViewById(R.id.fetch_skills);

        // Create request objects
        jobs_queue = Volley.newRequestQueue(this);
        skills_queue = Volley.newRequestQueue(this);

        // Set request objects
        fetch_jobs_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                get_jobs();
            }
        });

        fetch_skills_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                get_skills();
            }
        });
    }

    private void get_jobs() {
        String url = "http://192.168.100.65:8000/job_matching";
        JSONArray skills = new JSONArray();
        JSONObject json_body = new JSONObject();

        // Creates list of skills for request
        skills.put("communication");
        skills.put("teamwork");
        skills.put("problem_solving");

        // Creates request body
        try {
            json_body.put("match_percentage", 10);
            json_body.put("skills", skills);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST, url, json_body,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            JSONArray all_jobs_percentages = response.getJSONArray("result");
                            for (int i=0; i < all_jobs_percentages.length(); i++) {
                                JSONArray job_percentage = all_jobs_percentages.getJSONArray(i);
                                String job = job_percentage.getString(0);
                                int percentage = job_percentage.getInt(1);
                                jobs_result.append("Job: " + job + " | Percentage: " + percentage + "\n");
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                }, new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        error.printStackTrace();
                    }
        }){
            @NonNull
            @Override
            public Map<String,String> getHeaders() throws AuthFailureError {
                // build headers
                final Map <String,String> params = new HashMap<>();
                params.put("Content-Type", "application/json");
                return params;
            }
        };

        jobs_queue.add(request);
    }

    private void get_skills() {
        String url = "http://192.168.100.65:8000/skills";
        JSONObject json_body = new JSONObject();

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST, url, json_body,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        skills_result.setText("Skills:\n\n");
                        try {
                            JSONArray skills = response.getJSONArray("result");
                            for (int i=0; i < skills.length(); i++) {
                                String skill = skills.getString(i);

                                if (i+1 == skill.length()) {
                                    skills_result.append(skill);
                                } else {
                                    skills_result.append(skill + ", ");
                                }
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                error.printStackTrace();
            }
        }){
            @NonNull
            @Override
            public Map<String,String> getHeaders() throws AuthFailureError {
                // build headers
                final Map <String,String> params = new HashMap<>();
                params.put("Content-Type", "application/json");
                return params;
            }
        };

        skills_queue.add(request);
    }
}