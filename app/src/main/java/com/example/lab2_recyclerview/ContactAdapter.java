package com.example.lab2_recyclerview;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.imageview.ShapeableImageView;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;

public class ContactAdapter extends RecyclerView.Adapter<ContactAdapter.ContactViewHolder>{
    private ArrayList<ContactDataModel> data;

    public class ContactViewHolder extends RecyclerView.ViewHolder {
        TextView name_textview;
        TextView email_textview;
        TextView phone_textview;
        ShapeableImageView photo_imageview;

        public ContactViewHolder(View v){
            super(v);

            this.name_textview = v.findViewById(R.id.tv_name);
            this.email_textview = v.findViewById(R.id.tv_email);
            this.phone_textview = v.findViewById(R.id.tv_phone);
            this.photo_imageview = v.findViewById(R.id.iv_photo);
        }
    }

    public ContactAdapter(ArrayList<ContactDataModel> l){
        this.data = l;
    }

    @Override
    public int getItemCount() {
        return data.size();
    }

    @NonNull
    @Override
    public ContactViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.contact_card, parent, false);
        view.setOnClickListener(MainActivity.listener);

        return new ContactViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ContactViewHolder holder, int position) {
        TextView name_textview = holder.name_textview;
        TextView email_textview = holder.email_textview;
        TextView phone_textview = holder.phone_textview;
        ShapeableImageView photo_imageview = holder.photo_imageview;

        final String name = data.get(position).getName();

        name_textview.setText(name);
        email_textview.setText(data.get(position).getEmailAddress());
        phone_textview.setText(data.get(position).getPhoneNumber());
        Picasso.get().load(data.get(position).getImageUrl()).placeholder(new CircularProgressIndicator(holder.itemView.getContext()).getIndeterminateDrawable()).into(photo_imageview);
    }
}
