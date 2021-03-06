package com.mikerinehart.rideguide.adapters;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.mikerinehart.rideguide.R;
import com.mikerinehart.rideguide.RoundedTransformation;
import com.mikerinehart.rideguide.models.Notification;
import com.mikerinehart.rideguide.models.Reservation;
import com.mikerinehart.rideguide.models.Shift;
import com.mikerinehart.rideguide.models.User;
import com.squareup.picasso.Picasso;
import com.timehop.stickyheadersrecyclerview.StickyRecyclerHeadersAdapter;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class NotificationsAdapter extends RecyclerView.Adapter<NotificationsAdapter.NotificationsAdapterViewHolder> {

    private List<Notification> notificationList;
    private Context c;

    private final String TAG = "NotificationsAdapter";

    public NotificationsAdapter(List<Notification> notificationList) {
        this.notificationList = notificationList;
    }

    @Override
    public int getItemCount() {
        if (notificationList != null)
        {
            return notificationList.size();
        } else {
            return 0;
        }
    }

    @Override
    public void onBindViewHolder(NotificationsAdapterViewHolder vh, int i) {
        DateFormat df = new SimpleDateFormat("E d, h:mma");
        Notification n = notificationList.get(i);
        vh.message.setText(n.getMessage());
        vh.date.setText(df.format(n.getDate()));

        if (n.isSeen() != null && n.isSeen()) {
            vh.seen.setVisibility(TextView.VISIBLE);
        }

        Picasso.with(vh.userPic.getContext())
                .load("https://graph.facebook.com/" + n.getFbUid() + "/picture?type=large")
                .transform(new RoundedTransformation(100, 5))
                .into(vh.userPic);
    }


    @Override
    public NotificationsAdapterViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
        this.c = viewGroup.getContext();
        View itemView = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.notification_list_item, viewGroup, false);
        return new NotificationsAdapterViewHolder(itemView);
    }

    //Holds the Ride cardviews
    public static class NotificationsAdapterViewHolder extends RecyclerView.ViewHolder {

        protected TextView message;
        protected TextView date;
        protected ImageView userPic;
        protected TextView seen;

        public NotificationsAdapterViewHolder(View v) {
            super(v);
            message = (TextView) v.findViewById(R.id.notification_list_item_message);
            date = (TextView) v.findViewById(R.id.notification_list_item_date);
            userPic = (ImageView) v.findViewById(R.id.notification_list_item_user_pic);
            seen = (TextView) v.findViewById(R.id.notification_list_item_seen);
        }

    }

}
