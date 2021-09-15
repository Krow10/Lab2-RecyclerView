package com.example.lab2_recyclerview;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.RotateAnimation;
import android.widget.ImageView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Locale;
import java.util.Random;

public class MainActivity extends AppCompatActivity {
    public ArrayList<ContactDataModel> contacts;
    public ArrayList<ContactDataModel> deleted_contacts;

    ExtendedFloatingActionButton restore_fab;

    RecyclerView contacts_recyclerview;
    ContactAdapter adapter;
    public static ContactCardClickListener listener;

    class ContactCardClickListener implements View.OnClickListener {
        private final Context context;

        private ContactCardClickListener(Context context) {
            this.context = context;
        }

        @Override
        public void onClick(View v) {
            removeItem(v);
        }

        private void removeItem(View v) {
            final int selectedItemPosition = contacts_recyclerview.getChildAdapterPosition(v);

            try {
                ContactDataModel removed_contact = contacts.remove(selectedItemPosition);
                deleted_contacts.add(removed_contact);
                adapter.notifyItemRemoved(selectedItemPosition);

                // Re-enable button since we have contact to restore
                if (!restore_fab.isEnabled()) {
                    restore_fab.extend();
                    restore_fab.setEnabled(true);
                }
            } catch (ArrayIndexOutOfBoundsException e) {
                System.err.println("Error removing contact : " + e.toString());
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        MaterialToolbar app_bar = findViewById(R.id.appbar);
        setSupportActionBar(app_bar);

        // Setup recycler view
        contacts_recyclerview = this.findViewById(R.id.contacts_recyclerview);
        contacts_recyclerview.setLayoutManager(new SnappingLinearLayoutManager(this));
        listener = new ContactCardClickListener(this);

        deleted_contacts = new ArrayList<ContactDataModel>();
        contacts = new ArrayList<ContactDataModel>();

        adapter = new ContactAdapter(contacts);
        contacts_recyclerview.setAdapter(adapter);

        initializeData();

        restore_fab = findViewById(R.id.restore_fab);
        restore_fab.shrink();
        restore_fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {
                    ContactDataModel restored_contact = deleted_contacts.remove(deleted_contacts.size() - 1);
                    contacts.add(0, restored_contact);
                    adapter.notifyItemInserted(0);
                    contacts_recyclerview.smoothScrollToPosition(0); // Scroll back to top

                    // If 'trash bin' is empty, disable the restore button
                    if (deleted_contacts.isEmpty()) {
                        restore_fab.shrink();
                        restore_fab.setEnabled(false);
                    }
                } catch (IndexOutOfBoundsException e) {
                    System.out.println("Trash bin empty");
                }
            }
        });
    }

    // Setup refresh icon animation
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.top_bar_menu, menu);

        MenuItem refresh_item = menu.findItem(R.id.action_refresh_contacts);
        ImageView iv = (ImageView) refresh_item.getActionView();

        iv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                initializeData();

                Animation refresh_anim = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.refresh_contacts_rotate);
                refresh_anim.setAnimationListener(new Animation.AnimationListener() {
                    @Override
                    public void onAnimationStart(Animation animation) {}

                    @Override
                    public void onAnimationEnd(Animation animation) {
                        v.setEnabled(true);
                    }

                    @Override
                    public void onAnimationRepeat(Animation animation) {}
                });
                v.startAnimation(refresh_anim);
                v.setEnabled(false);
            }
        });

        return super.onCreateOptionsMenu(menu);
    }

    private void initializeData(){
        deleted_contacts.clear();
        adapter.notifyItemRangeRemoved(0, contacts.size()); // Crucial to notify before clearing else 'size()' is zero and crashes app
        contacts.clear();

        // Setup API request for random names
        RequestQueue queue = Volley.newRequestQueue(this);
        final int max_generated_contacts = getResources().getInteger(R.integer.max_generated_contacts);
        String url = "http://names.drycodes.com/" + max_generated_contacts + "?nameOptions=boy_names"; // Can't mix boy/girl names... (requires multiple API calls)

        JsonArrayRequest jsonArrayRequest = new JsonArrayRequest
                (Request.Method.GET, url, null, new Response.Listener<JSONArray>() {
                    @Override
                    public void onResponse(JSONArray response) {
                        for (int i = 0; i < max_generated_contacts; i++){
                            try {
                                final String[] split_result = response.get(i).toString().split("_");
                                final String name = split_result[0];
                                final String surname = split_result[1];
                                final String phone = generatePhoneNumber();

                                contacts.add(0, new ContactDataModel(name + " " + surname,
                                        name.toLowerCase(Locale.ROOT) + "." + surname.toLowerCase(Locale.ROOT) + "@etsmtl.ca",
                                        phone, "person"));
                                adapter.notifyItemInserted(0);
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }, new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        System.err.println("Error fetching data  : " + error.getMessage());
                    }
                });

        queue.add(jsonArrayRequest);
    }

    // Original from @joeyv (https://gist.github.com/joeyv/7087747)
    private String generatePhoneNumber(){
        int num1, num2, num3; // Area code
        int set2, set3;

        Random generator = new Random();

        num1 = generator.nextInt(7) + 1; // Add one so there is no zero to begin
        num2 = generator.nextInt(8);
        num3 = generator.nextInt(8);

        set2 = generator.nextInt(643) + 100;
        set3 = generator.nextInt(8999) + 1000;

        return "(" + num1 + "" + num2 + "" + num3 + ")" + "-" + set2 + "-" + set3;
    }
}
