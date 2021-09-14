package com.example.lab2_recyclerview;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Context;
import android.os.Bundle;
import android.view.View;

import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {
    public ArrayList<ContactDataModel> contacts;
    public ArrayList<ContactDataModel> deleted_contacts;
    RecyclerView contacts_recyclerview;
    ExtendedFloatingActionButton restore_fab;
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
            int selectedItemPosition = contacts_recyclerview.getChildAdapterPosition(v);
            ContactDataModel removed_contact = contacts.remove(selectedItemPosition);
            deleted_contacts.add(removed_contact);
            adapter.notifyItemRemoved(selectedItemPosition);

            if (!restore_fab.isEnabled()) {
                restore_fab.extend();
                restore_fab.setEnabled(true);
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        contacts_recyclerview =  (RecyclerView) this.findViewById(R.id.contacts_recyclerview);
        contacts_recyclerview.setLayoutManager(new LinearLayoutManager(this));
        listener = new ContactCardClickListener(this);

        initializeData();
        adapter = new ContactAdapter(contacts);
        contacts_recyclerview.setAdapter(adapter);

        restore_fab = findViewById(R.id.restore_fab);
        restore_fab.shrink();
        restore_fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {
                    ContactDataModel restored_contact = deleted_contacts.remove(deleted_contacts.size() - 1);
                    contacts.add(0, restored_contact);
                    adapter.notifyItemInserted(0);

                    if (deleted_contacts.isEmpty()) {
                        restore_fab.shrink();
                        restore_fab.setEnabled(false);
                    }
                } catch (IndexOutOfBoundsException e) {
                    // TODO : Add toast notification for user
                    System.out.println("Trash bin empty");
                }
            }
        });
    }

    private void initializeData(){
        deleted_contacts = new ArrayList<ContactDataModel>();
        contacts = new ArrayList<ContactDataModel>();
        contacts.add(new ContactDataModel("Diala", "diala.naboulsi@etsmtl.ca", "(514) 396- 8800", "person"));
        contacts.add(new ContactDataModel("Tintin", "tintin@milou.ca", "(514) 396-8800", "person"));
        contacts.add(new ContactDataModel("Tom", "tom@jerry.ca", "(514) 396-8800", "person"));
    }
}
