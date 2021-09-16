package com.example.lab2_recyclerview;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.FrameLayout;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.badge.BadgeDrawable;
import com.google.android.material.badge.BadgeUtils;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.Locale;
import java.util.Random;
import java.util.function.Function;

public class MainActivity extends AppCompatActivity {
    public ArrayList<ContactDataModel> contacts;
    public ArrayList<ContactDataModel> deleted_contacts;

    ExtendedFloatingActionButton restore_fab;
    BadgeDrawable trash_counter;

    RecyclerView contacts_recyclerview;
    ContactAdapter adapter;
    public static ContactCardClickListener listener;

    class ContactCardClickListener implements View.OnClickListener {
        private ContactCardClickListener() {}

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
                updateTrashCounter(deleted_contacts.size());

                // Re-enable button since we have contact to restore
                if (!restore_fab.isEnabled())
                    setRestoreFabEnabled(true);
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

        contacts = new ArrayList<>();
        deleted_contacts = new ArrayList<>();
        listener = new ContactCardClickListener();
        adapter = new ContactAdapter(contacts);
        trash_counter = BadgeDrawable.create(this);

        contacts_recyclerview = this.findViewById(R.id.contacts_recyclerview);
        contacts_recyclerview.setLayoutManager(new SnappingLinearLayoutManager(this));
        contacts_recyclerview.setItemAnimator(null);
        contacts_recyclerview.setAdapter(adapter);

        restore_fab = findViewById(R.id.restore_fab);
        restore_fab.setOnClickListener((View view) -> {
            try {
                ContactDataModel restored_contact = deleted_contacts.remove(deleted_contacts.size() - 1);
                contacts.add(0, restored_contact);
                adapter.notifyItemInserted(0);
                updateTrashCounter(deleted_contacts.size());
                contacts_recyclerview.smoothScrollToPosition(0); // Scroll back to top

                // If 'trash bin' is empty, disable the restore button
                if (deleted_contacts.isEmpty())
                    setRestoreFabEnabled(false);
            } catch (IndexOutOfBoundsException e) {
                System.out.println("Trash bin empty");
            }
        });

        initializeData();
    }

    @SuppressLint("UnsafeExperimentalUsageError")
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.top_bar_menu, menu);

        MenuItem trash_bin_item = menu.findItem(R.id.action_trash_bin);
        FrameLayout trash_fl = (FrameLayout) trash_bin_item.getActionView();
        ImageView trash_iv = trash_fl.findViewById(R.id.iv_trash);

        // Offset the badge (converted from dp) to prevent clipping when count gets bigger
        final Function<Float, Integer> dpToPixels = (dip) -> Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dip, getResources().getDisplayMetrics()));
        trash_counter.setHorizontalOffset(dpToPixels.apply(3.5f));
        trash_counter.setVerticalOffset(dpToPixels.apply(.5f));
        trash_counter.setAlpha(0); // For some reason 'setVisible(false)' is not working when app is starting up so use alpha to hide the empty red dot instead

        // Setup trash counter badge to attach to trash icon
        trash_fl.setForeground(trash_counter);
        trash_fl.addOnLayoutChangeListener((v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) -> BadgeUtils.attachBadgeDrawable(trash_counter, trash_iv, trash_fl));

        // Setup refresh icon animation
        MenuItem refresh_item = menu.findItem(R.id.action_refresh_contacts);
        ImageView iv = (ImageView) refresh_item.getActionView();
        iv.setOnClickListener((View v) -> {
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
        });

        return super.onCreateOptionsMenu(menu);
    }

    private void initializeData(){
        adapter.notifyItemRangeRemoved(0, contacts.size()); // Crucial to notify before clearing else 'size()' is zero and crashes app
        contacts.clear();
        deleted_contacts.clear();
        setRestoreFabEnabled(false);
        updateTrashCounter(0); // Reset trash badge

        // Setup API request for random names
        RequestQueue queue = Volley.newRequestQueue(this);
        final int max_generated_contacts = getResources().getInteger(R.integer.max_generated_contacts);
        String url = "http://names.drycodes.com/" + max_generated_contacts + "?nameOptions=boy_names"; // Can't mix boy/girl names... (requires multiple API calls)

        JsonArrayRequest jsonArrayRequest = new JsonArrayRequest
                (Request.Method.GET, url, null, (JSONArray response) -> {
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
                }, error -> System.err.println("Error fetching data  : " + error.getMessage()));

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

    private void setRestoreFabEnabled(final boolean enabled){
        if (enabled){
            restore_fab.extend();
            restore_fab.setEnabled(true);
        } else {
            restore_fab.shrink();
            restore_fab.setEnabled(false);
        }
    }

    private void updateTrashCounter(final int count){
        // TODO : Add animation for trash counter update (fade in/out + expand/shrink instead of setVisible)
        if (count > 0) {
            if (!trash_counter.isVisible() || trash_counter.getAlpha() == 0) {
                trash_counter.setAlpha(255); // Restore alpha from app start-up
                trash_counter.setVisible(true);
            }

            trash_counter.setNumber(count);
        } else {
            trash_counter.setVisible(false);
        }
    }
}
