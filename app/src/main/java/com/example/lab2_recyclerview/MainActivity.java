package com.example.lab2_recyclerview;

import android.annotation.SuppressLint;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.AnimatedVectorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.VectorDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.FrameLayout;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.res.ResourcesCompat;
import androidx.dynamicanimation.animation.DynamicAnimation;
import androidx.dynamicanimation.animation.SpringAnimation;
import androidx.dynamicanimation.animation.SpringForce;
import androidx.interpolator.view.animation.FastOutSlowInInterpolator;
import androidx.interpolator.view.animation.LinearOutSlowInInterpolator;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.SimpleItemAnimator;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.badge.BadgeDrawable;
import com.google.android.material.badge.BadgeUtils;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {
    public ArrayList<ContactDataModel> contacts;
    public ArrayList<ContactDataModel> deleted_contacts;
    public static ContactCardClickListener listener;

    private ExtendedFloatingActionButton restore_fab;
    private BadgeDrawable trash_counter;

    private RecyclerView contacts_recyclerview;
    private ContactAdapter adapter;

    // Setup contact deletion when clicking on contact card
    class ContactCardClickListener implements View.OnClickListener {
        private ContactCardClickListener() {}

        @Override
        public void onClick(View v) { removeItem(v); }
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
        contacts_recyclerview.setAdapter(adapter);
        // Setup animations for contact cards
        contacts_recyclerview.setItemAnimator(new SimpleItemAnimator() {
            @Override
            public boolean animateRemove(RecyclerView.ViewHolder holder) {
                View item_view = holder.itemView;
                View trash_icon_view = findViewById(R.id.action_trash_bin);

                // Check if item was deleted from click or from swipe
                final boolean item_swiped = Math.abs(item_view.getX()) > 0;

                if (!item_swiped) { // Animate card into trash bin
                    final int anim_duration = getResources().getInteger(R.integer.contact_to_trash_anim_speed);
                    final int[] trash_icon_coords = new int[2];
                    trash_icon_view.getLocationOnScreen(trash_icon_coords);

                    // Calculate distances from card to trash icon
                    final float[] translate_values = new float[]{Math.abs(trash_icon_coords[0] - trash_icon_view.getWidth() / 3f - (item_view.getX() + item_view.getWidth())),
                            Math.abs(trash_icon_coords[1] - trash_icon_view.getHeight() / 3f - (item_view.getY() + item_view.getHeight()))};

                    setAppBarExpanded(true); // Show app bar for the trash icon
                    item_view.animate()
                            .translationXBy(translate_values[0])
                            .translationYBy(-translate_values[1]) // Minus sign for translating upward
                            .translationZ(2f) // Bring animated card in front of others
                            .scaleX(0f)
                            .scaleY(0f)
                            .setDuration(anim_duration)
                            .setInterpolator(new LinearOutSlowInInterpolator())
                            // Start trash badge update (with bounce animation) a bit later for smoother transition with trash opening
                            .withStartAction(() -> runDelayed(() -> updateTrashCounter(deleted_contacts.size()), Math.round(1.2 * anim_duration)))
                            // Remove the view from screen
                            .withEndAction(() -> item_view.setVisibility(View.GONE))
                            .start();

                    // Adjust trash can opening to wait for the item to move closer to the icon
                    runDelayed(() -> startTrashCanOpeningAnimation(), Math.round(0.3 * anim_duration));
                } else {
                    updateTrashCounter(deleted_contacts.size());
                }

                return false;
            }

            @Override
            public boolean animateAdd(RecyclerView.ViewHolder holder) {
                View item_view = holder.itemView;
                final int starting_pos = getResources().getInteger(R.integer.contact_add_fade_in_start_position);
                final int anim_duration = getResources().getInteger(R.integer.contact_add_fade_in_anim_speed);
                final int position_factor_duration = getResources().getInteger(R.integer.contact_add_fade_in_position_speed_factor);
                final int item_pos = holder.getLayoutPosition() + 1;

                item_view.setAlpha(0f);
                item_view.setTranslationY(-starting_pos); // Make card appear from top

                item_view.animate()
                    .alpha(1f) // Fade-in animation
                    .translationYBy(starting_pos)
                    .setDuration(anim_duration + (long) item_pos *position_factor_duration) // Longer duration for item further down the list (unfolding effect)
                    .setInterpolator(new FastOutSlowInInterpolator())
                    .start();

                return false;
            }

            @Override
            public boolean animateMove(RecyclerView.ViewHolder holder, int fromX, int fromY, int toX, int toY) {
                View item_view = holder.itemView;
                final int duration = getResources().getInteger(R.integer.contact_move_slide_anim_speed);

                /*
                    Problem : If user is scrolling while the recycler view is moving items, they end up being misaligned potentially on top of each other, ...

                    'Dirty' solution : Prevent user from scrolling until items are moved be even that doesn't prevent all cases if user is spamming
                                       the card deletion and trying to scroll at the same time.
                 */
                ((SnappingLinearLayoutManager)(contacts_recyclerview.getLayoutManager())).setScrollEnabled(false);

                // Sliding animation for items re-arranging after item remove
                item_view.setY(fromY);
                item_view.animate()
                        .y(toY)
                        .setDuration(duration)
                        .setInterpolator(new FastOutSlowInInterpolator())
                        .start();

                runDelayed(() -> ((SnappingLinearLayoutManager)(contacts_recyclerview.getLayoutManager())).setScrollEnabled(true), duration); // Restore scroll after animation

                return false;
            }

            @Override
            public boolean animateChange(RecyclerView.ViewHolder oldHolder, RecyclerView.ViewHolder newHolder, int fromLeft, int fromTop, int toLeft, int toTop) { return false; }

            @Override
            public void runPendingAnimations() {}

            @Override
            public void endAnimation(@NonNull RecyclerView.ViewHolder item) {}

            @Override
            public void endAnimations() {}

            @Override
            public boolean isRunning() { return false; }
        });

        // Setup delete on item swipe (left or right)
        new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) { return false; }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int swipeDir) { removeItem(viewHolder.itemView); }

            @Override
            public void clearView(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
                super.clearView(recyclerView, viewHolder);
                viewHolder.itemView.setAlpha(1.f); // Restore card transparency if user releases swipe but do not delete item
            }

            @Override
            public void onChildDraw(@NonNull Canvas c, @NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, float dX, float dY, int actionState, boolean isCurrentlyActive) {
                if (actionState == ItemTouchHelper.ACTION_STATE_SWIPE && isCurrentlyActive) {
                    View itemView = viewHolder.itemView;

                    // Setup swiped card background (red color + trash icon)
                    VectorDrawable swipe_delete_background_icon = (VectorDrawable) ResourcesCompat.getDrawable(getResources(), R.drawable.ic_baseline_delete_24, null);
                    GradientDrawable swipe_delete_background_fill = new GradientDrawable();
                    swipe_delete_background_fill.setBounds(
                            Math.round(itemView.getLeft()),
                            Math.round(itemView.getTop() + dpToPixels(3)), // Small offset for the margin between items in recycler view
                            Math.round(itemView.getLeft() + itemView.getWidth()),
                            Math.round(itemView.getTop() + itemView.getHeight() - dpToPixels(3)));
                    swipe_delete_background_fill.setColor(ResourcesCompat.getColor(getResources(), R.color.red, null));

                    Rect back_coords = swipe_delete_background_fill.getBounds();
                    final boolean is_swiping_to_right = dX > 0;
                    final float icon_scale = 1.5f;

                    // Calculate horizontal  offset from background depending on swipe direction
                    final float x_offset = (is_swiping_to_right ? 1 : -1) * (back_coords.centerY()
                            - back_coords.top
                            - swipe_delete_background_icon.getIntrinsicHeight()
                            + (is_swiping_to_right ? 0 : 1) * icon_scale * swipe_delete_background_icon.getIntrinsicWidth());

                    swipe_delete_background_icon.setBounds(
                            Math.round((is_swiping_to_right ? back_coords.left : back_coords.right) + x_offset),
                            Math.round(back_coords.centerY() - icon_scale*swipe_delete_background_icon.getIntrinsicHeight()/2),
                            Math.round((is_swiping_to_right ? back_coords.left : back_coords.right) + icon_scale*swipe_delete_background_icon.getIntrinsicWidth() + x_offset),
                            Math.round(back_coords.centerY() + icon_scale*swipe_delete_background_icon.getIntrinsicHeight()/2));

                    swipe_delete_background_fill.draw(c);
                    swipe_delete_background_icon.draw(c);

                    // Slide the card in the swipe direction and make it more transparent as it gets closer to edge of screen
                    final float alpha = 1.f - Math.abs(dX) / (float) itemView.getWidth();
                    itemView.setAlpha(alpha);
                    itemView.setTranslationX(dX);
                } else {
                    super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
                }
            }
        }).attachToRecyclerView(contacts_recyclerview);

        // Setup contact restore from Floating Action Button
        restore_fab = findViewById(R.id.restore_fab);
        restore_fab.setOnClickListener((View view) -> {
            try {
                ContactDataModel restored_contact = deleted_contacts.remove(deleted_contacts.size() - 1);
                contacts.add(0, restored_contact);
                adapter.notifyItemInserted(0);

                // Trash can icon animation and update
                startTrashCanOpeningAnimation();
                updateTrashCounter(deleted_contacts.size());

                /*
                    Problem : Sometimes item is not finished inserting before starting scroll which cause card 'add' animation to not be rendered.
                              This is due to the insert and scroll occurring in different threads.

                    Solution : None really viable.
                 */
                contacts_recyclerview.smoothScrollToPosition(0); // Scroll back to top
                setAppBarExpanded(true); // Show app bar to stay consistent with manual scroll behavior

                // If 'trash bin' is empty, disable the restore button
                if (deleted_contacts.isEmpty())
                    setRestoreFabEnabled(false);
            } catch (IndexOutOfBoundsException e) {
                System.err.println("Trash bin empty");
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
        trash_counter.setHorizontalOffset(dpToPixels(3.5f));
        trash_counter.setVerticalOffset(dpToPixels(.5f));
        trash_counter.setBadgeTextColor(ResourcesCompat.getColor(getResources(), R.color.white, null));
        trash_counter.setBackgroundColor(ResourcesCompat.getColor(getResources(), R.color.red, null));
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
                public void onAnimationEnd(Animation animation) { v.setEnabled(true); }

                @Override
                public void onAnimationRepeat(Animation animation) {}
            });

            v.startAnimation(refresh_anim);
            v.setEnabled(false); // Prevent refresh spamming
        });

        return super.onCreateOptionsMenu(menu);
    }

    private void initializeData(){
        adapter.notifyItemRangeRemoved(0, contacts.size()); // Crucial to notify before clearing else 'size()' is zero and crashes app
        contacts.clear();
        deleted_contacts.clear();
        setRestoreFabEnabled(false);
        updateTrashCounter(0); // Reset trash badge counter

        // Setup API request for random contact data
        RequestQueue queue = Volley.newRequestQueue(this);
        final int max_generated_contacts = getResources().getInteger(R.integer.max_generated_contacts);
        String url = "https://randomuser.me/api?noinfo&nat=ca,us,fr&inc=name,picture,email,cell&results=" + max_generated_contacts;

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest
                (Request.Method.GET, url, null, (JSONObject response) -> {
                    try {
                        final JSONArray results = (JSONArray) response.get("results");
                        for (int i = 0; i < max_generated_contacts; i++) {
                            final JSONObject person = (JSONObject) results.get(i);
                            final JSONObject name = (JSONObject) person.get("name");
                            final JSONObject pic = (JSONObject) person.get("picture");

                            contacts.add(0, new ContactDataModel(name.getString("first") + " " + name.getString("last"),
                                    person.getString("email"), person.getString("cell"), pic.getString("large")));
                            adapter.notifyItemInserted(0);
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }, error -> System.err.println("Error fetching data  : " + error.getMessage()));

        queue.add(jsonObjectRequest);
    }

    private void removeItem(View v) {
        final int selectedItemPosition = contacts_recyclerview.getChildAdapterPosition(v);

        try {
            ContactDataModel removed_contact = contacts.remove(selectedItemPosition);
            deleted_contacts.add(removed_contact);
            adapter.notifyItemRemoved(selectedItemPosition);

            // Re-enable button since we have contact to restore
            if (!restore_fab.isEnabled())
                setRestoreFabEnabled(true);
        } catch (ArrayIndexOutOfBoundsException e) {
            System.err.println("Error removing contact : " + e.toString());
        }
    }

    private void setRestoreFabEnabled(final boolean enabled) {
        if (enabled) {
            restore_fab.extend();
            restore_fab.setEnabled(true);
        } else {
            restore_fab.shrink();
            restore_fab.setEnabled(false);
        }
    }

    private void setAppBarExpanded(final boolean expanded) {
        AppBarLayout l = findViewById(R.id.appbar_layout);
        l.setExpanded(expanded);
    }

    private void updateTrashCounter(final int count) {
        if (count > 0) {
            if (!trash_counter.isVisible() || trash_counter.getAlpha() == 0) {
                trash_counter.setAlpha(255); // Restore alpha from app start-up
                trash_counter.setVisible(true);
            }

            // Show bounce animation if new contact added to trash bin
            if (count > trash_counter.getNumber())
                startBounceTrashAnimation();

            trash_counter.setNumber(count);
        } else {
            trash_counter.setVisible(false);
            trash_counter.setNumber(0);
        }
    }

    private void startBounceTrashAnimation() {
        View v = findViewById(R.id.action_trash_bin);
        final float bounce_height = getResources().getDimension(R.dimen.trash_notification_bounce_height);
        final float icon_center_offset = v.findViewById(R.id.iv_trash).getHeight() / (2 * getResources().getDisplayMetrics().density); // Re-center icon after bounce animation

        // SpringAnimation for bouncing effect
        final SpringAnimation springAnim = new SpringAnimation(v, DynamicAnimation.Y, -bounce_height);
        springAnim.getSpring().setStiffness(SpringForce.STIFFNESS_LOW);
        springAnim.getSpring().setDampingRatio(SpringForce.DAMPING_RATIO_LOW_BOUNCY);
        springAnim.addEndListener(new DynamicAnimation.OnAnimationEndListener() {
            @Override
            public void onAnimationEnd(DynamicAnimation animation, boolean canceled, float value, float velocity) {
                springAnim.removeEndListener(this); // Remove listener to prevent bounce loop
                springAnim.setStartValue(-bounce_height);
                springAnim.animateToFinalPosition(icon_center_offset);
            }
        });

        springAnim.start(); // First animate up then down with end listener
    }

    private void startTrashCanOpeningAnimation() {
        AnimatedVectorDrawable trash_icon_vector = (AnimatedVectorDrawable) ((ImageView)(findViewById(R.id.iv_trash))).getDrawable();
        trash_icon_vector.start();
    }

    private Handler runDelayed(final Runnable func, final long delay) {
        final Handler handler = new Handler();
        handler.postDelayed(func, delay);

        return handler;
    }

    private int dpToPixels(float dip) {
        return Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dip, getResources().getDisplayMetrics()));
    }
}
