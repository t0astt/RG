package com.mikerinehart.rideguide.main_fragments;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.materialdialogs.MaterialDialog;
import com.gc.materialdesign.views.ButtonFloat;
import com.gc.materialdesign.views.ProgressBarCircularIndeterminate;
import com.github.amlcurran.showcaseview.ShowcaseView;
import com.github.amlcurran.showcaseview.targets.ViewTarget;
import com.github.jjobes.slidedatetimepicker.SlideDateTimeListener;
import com.github.jjobes.slidedatetimepicker.SlideDateTimePicker;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.loopj.android.http.RequestParams;
import com.mikerinehart.rideguide.R;
import com.mikerinehart.rideguide.RestClient;
import com.mikerinehart.rideguide.SimpleDividerItemDecoration;
import com.mikerinehart.rideguide.activities.Constants;
import com.mikerinehart.rideguide.activities.MainActivity;
import com.mikerinehart.rideguide.adapters.MyShiftsAdapter;
import com.mikerinehart.rideguide.models.Shift;
import com.mikerinehart.rideguide.models.User;
import com.timehop.stickyheadersrecyclerview.StickyRecyclerHeadersDecoration;

import org.apache.http.Header;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Type;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

public class MyShiftsFragment extends Fragment {
    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static User ARG_PARAM1;
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private User me;
    private String mParam2;

    private OnFragmentInteractionListener mListener;

    private ButtonFloat newShiftButton;
    private TextView shiftShame;
    private ProgressBarCircularIndeterminate loadingIcon;
    private ButtonFloat createShiftButton;
    private SwipeRefreshLayout mSwipeRefreshLayout;
    private RecyclerView shiftList;
    private MyShiftsAdapter shiftsAdapter;
    private View v;

    SharedPreferences sp;
    private boolean showShiftsShowcase;

    private String TAG = "MyShiftsFragment";


    // TODO: Rename and change types and number of parameters
    public static MyShiftsFragment newInstance(User param1, String param2) {
        MyShiftsFragment fragment = new MyShiftsFragment();
        Bundle args = new Bundle();
        args.putParcelable("USER", param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    public MyShiftsFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        MainActivity.toolbar.setTitle("My Shifts");
        MainActivity.drawerAdapter.selectPosition(4);
        if (getArguments() != null) {
            me = getArguments().getParcelable("USER");
            mParam2 = getArguments().getString(ARG_PARAM2);
        }
        sp = getActivity().getSharedPreferences(Constants.PREFERENCES, Context.MODE_PRIVATE);
        showShiftsShowcase = sp.getBoolean(Constants.SHOWSHIFTSSHOWCASE, true); // True if need to show


    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        v = inflater.inflate(R.layout.fragment_my_shifts_page, container, false);

        shiftShame = (TextView)v.findViewById(R.id.myshifts_shift_shame);
        loadingIcon = (ProgressBarCircularIndeterminate)v.findViewById(R.id.myshifts_circular_loading);

        createShiftButton = (ButtonFloat)v.findViewById(R.id.myshifts_new_shift_fab);
        createShiftButton.setEnabled(true);
        createShiftButton.setRippleSpeed(100);
        createShiftButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                createShiftDialog();
            }
        });

        shiftList = (RecyclerView) v.findViewById(R.id.myshifts_my_shifts_list);
        shiftList.setHasFixedSize(true);
        shiftList.setAdapter(new MyShiftsAdapter());
        LinearLayoutManager llm = new LinearLayoutManager(shiftList.getContext());
        llm.setOrientation(LinearLayoutManager.VERTICAL);
        shiftList.setLayoutManager(llm);
        refreshContent();

        final GestureDetector mGestureDetector = new GestureDetector(getActivity(), new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onSingleTapUp(MotionEvent e) {
                return true;
            }
        });

        shiftList.addOnItemTouchListener(new RecyclerView.OnItemTouchListener() {
            @Override
            public boolean onInterceptTouchEvent(RecyclerView recyclerView, final MotionEvent motionEvent) {
                ViewGroup child = (ViewGroup) recyclerView.findChildViewUnder(motionEvent.getX(), motionEvent.getY());

                if (child != null && mGestureDetector.onTouchEvent(motionEvent)) {
                    final int itemClicked = recyclerView.getChildPosition(child);

                    final String[] dialogOptions = {"View Reservations", "Delete Shift"};
                    MaterialDialog shiftDialog = new MaterialDialog.Builder(MyShiftsFragment.this.getActivity())
                            .title("Shift Options")
                            .items(dialogOptions)
                            .itemsCallbackSingleChoice(0, new MaterialDialog.ListCallback() {
                                @Override
                                public void onSelection(MaterialDialog dialog, View view, int which, CharSequence text) {
                                    if (which == 0) {
                                        shiftsAdapter.createReservationsDialog(shiftsAdapter.getShift(itemClicked).getReservations(), false);
                                    } else if (which == 1) {
                                        new MaterialDialog.Builder(MyShiftsFragment.this.getActivity())
                                                .title("Confirm Shift Delete")
                                                .content("Are you sure you want to delete this shift? All reservations made after the current time will be deleted!")
                                                .positiveText("YES")
                                                .negativeText("NO")
                                                .callback(new MaterialDialog.ButtonCallback() {
                                                    @Override
                                                    public void onPositive(MaterialDialog dialog) {
                                                        RequestParams params = new RequestParams("shift_id", shiftsAdapter.getShift(itemClicked).getId());
                                                        RestClient.post("shifts/deleteShift", params, new JsonHttpResponseHandler() {
                                                            @Override
                                                            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                                                                try {
                                                                    if (response.getString("status").equalsIgnoreCase("success")) {
                                                                        Toast.makeText(MyShiftsFragment.this.getActivity(), "Shift Removed!", Toast.LENGTH_SHORT).show();
                                                                        refreshContent();
                                                                    } else {
                                                                        Toast.makeText(MyShiftsFragment.this.getActivity(), "Error, please try again!", Toast.LENGTH_SHORT).show();
                                                                        refreshContent();
                                                                    }
                                                                } catch (JSONException e) {
                                                                    e.printStackTrace();
                                                                }
                                                            }

                                                            @Override
                                                            public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONObject errorResponse) {
                                                                Log.i(TAG, "Error: " + errorResponse);
                                                                mSwipeRefreshLayout.setRefreshing(false);
                                                            }
                                                        });
                                                    }
                                                })
                                                .build()
                                                .show();

                                    }
                                }
                            })
                            .positiveText("OK")
                            .negativeText("CANCEL")
                            .build();
                            shiftDialog.show();

                }
                return false;
            }

            @Override
            public void onTouchEvent(RecyclerView recyclerView, MotionEvent motionEvent) {
                Log.i(TAG, "TouchEvent");
            }
        });

        return v;
    }

    /*
     * Returns false if no list items, true if list items present
     */
    private void refreshContent() {

        RequestParams params = new RequestParams("user_id", me.getId());
        RestClient.post("shifts/myShifts", params, new JsonHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONArray response) {
                List<Shift> result;
                Gson gson = new GsonBuilder().setDateFormat("yyyy-MM-dd HH:mm:ss").create();
                Type listType = new TypeToken<List<Shift>>() {
                }.getType();

                result = (List<Shift>)gson.fromJson(response.toString(), listType);

                // check whether or not to shame the user hehe
                if (result.size() == 0) {
                    shiftsAdapter = new MyShiftsAdapter(result, me);
                    shiftList.setAdapter(shiftsAdapter);
                    shiftShame.setVisibility(TextView.VISIBLE);
                    //shiftList.setVisibility(RecyclerView.GONE);
                    loadingIcon.setVisibility(ProgressBarCircularIndeterminate.GONE);
                } else {
                    shiftShame.setVisibility(TextView.GONE);
                    shiftList.setVisibility(RecyclerView.VISIBLE);
                    shiftsAdapter = new MyShiftsAdapter(result, me);

                    shiftList.addItemDecoration(new SimpleDividerItemDecoration(shiftList.getContext()));

                    loadingIcon.setVisibility(ProgressBarCircularIndeterminate.GONE);
                    shiftList.setAdapter(shiftsAdapter);
                    StickyRecyclerHeadersDecoration headersDecor = new StickyRecyclerHeadersDecoration(shiftsAdapter);
                    shiftList.addItemDecoration(headersDecor);
                }
                mSwipeRefreshLayout = (SwipeRefreshLayout)v.findViewById(R.id.myshifts_swipe_refresh_layout);
                mSwipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
                    @Override
                    public void onRefresh() {
                        //refreshContent();
                        getActivity().getSupportFragmentManager()
                                .beginTransaction()
                                .replace(R.id.container, MyShiftsFragment.newInstance(me, "ProfileFragment"))
                                .commit();
                    }
                });
                mSwipeRefreshLayout.setRefreshing(false);
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONObject errorResponse) {
                Log.i(TAG, "Error: " + errorResponse);
                mSwipeRefreshLayout.setRefreshing(false);
            }
        });
    }

    public void createShiftDialog() {

        final DateFormat displayFormat = new SimpleDateFormat("yyyy-MM-dd h:mma");

        MaterialDialog dialog = null;
        final EditText startTime;
        final EditText endTime;
        final EditText seats;

        LayoutInflater inflater = LayoutInflater.from(getActivity().getBaseContext());
        View dialogLayout = inflater.inflate(R.layout.myshifts_new_shift_dialog, null);

        startTime = (EditText)dialogLayout.findViewById(R.id.myshifts_new_shift_dialog_start_time);
        endTime = (EditText)dialogLayout.findViewById(R.id.myshifts_new_shift_dialog_end_time);
        seats = (EditText)dialogLayout.findViewById(R.id.myshifts_new_shift_dialog_num_seats);

        dialog = new MaterialDialog.Builder(MyShiftsFragment.this.getActivity())
                .title("Create Shift")
                .customView(dialogLayout)
                .positiveText("Create")
                .negativeText("Cancel")
                .callback(new MaterialDialog.ButtonCallback() {
                    @Override
                    public void onPositive(MaterialDialog dialog) {

                        RequestParams params = new RequestParams("user_id", me.getId());
                        params.put("seats", seats.getText());
                        params.put("start", startTime.getText());
                        params.put("end", endTime.getText());
                        RestClient.post("shifts", params, new JsonHttpResponseHandler() {
                            @Override
                            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                                //refreshContent();
                                getActivity().getSupportFragmentManager()
                                        .beginTransaction()
                                        .replace(R.id.container, MyShiftsFragment.newInstance(me, "RidesFragment"))
                                        .commit();
                                Toast.makeText(getActivity().getApplicationContext(), "Shift created!", Toast.LENGTH_LONG).show();
                            }

                            @Override
                            public void onFailure(int statusCode, Header[] headers, String response, Throwable throwable) {
                                Log.i(TAG, "Error " + statusCode + ": " + response);
                                Toast.makeText(getActivity().getApplicationContext(), "Error, please try again", Toast.LENGTH_LONG).show();
                            }
                        });
                    }
                })
                .build();
        dialog.show();

        final SlideDateTimeListener startListener = new SlideDateTimeListener() {
            @Override
            public void onDateTimeSet(Date date) {
                startTime.setText(displayFormat.format(date));
            }
        };

        final SlideDateTimeListener endListener = new SlideDateTimeListener() {
            @Override
            public void onDateTimeSet(Date date) {
                endTime.setText(displayFormat.format(date));
            }
        };

        startTime.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Date d = new Date();
                GregorianCalendar calendar = new GregorianCalendar();
                calendar.setTime(d);
                calendar.add(Calendar.DATE, 7);
                new SlideDateTimePicker.Builder(getFragmentManager())
                        .setListener(startListener)
                        .setInitialDate(d)
                        .setMinDate(d)
                        .setMaxDate(calendar.getTime())
                        .build()
                        .show();
            }
        });

        endTime.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                    Date d = new Date();
                    GregorianCalendar calendar = new GregorianCalendar();
                    calendar.setTime(d);
                    calendar.add(Calendar.DATE, 7);
                    new SlideDateTimePicker.Builder(getFragmentManager())
                            .setListener(endListener)
                            .setInitialDate(d)
                                    .setMinDate(d)
                                    .setMaxDate(calendar.getTime())
                                    .build()
                                    .show();
            }
        });

    }

    private void showcase() {
        ViewTarget target = new ViewTarget(R.id.myshifts_new_shift_fab, getActivity());
        ShowcaseView sv = new ShowcaseView.Builder(getActivity(), true)
                .setTarget(target)
                .setContentTitle("My Shifts")
                .setContentText("Want to be a designated driver? Click this button to create a shift, letting people know" +
                        " you're available to drive!\n\n\n" +
                        "After your shift is over it will be moved to My History.")
                .hideOnTouchOutside()
                .setStyle(R.style.CustomShowcaseTheme2)
                .build();
        RelativeLayout.LayoutParams lps = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lps.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
        lps.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
        int margin = ((Number) (getResources().getDisplayMetrics().density * 12)).intValue();
        lps.setMargins(margin, margin, margin, margin);
        sv.setButtonPosition(lps);
    }

    public void onResume() {
        super.onResume();
        if (showShiftsShowcase) {
            SharedPreferences.Editor editor = sp.edit();
            editor.putBoolean(Constants.SHOWSHIFTSSHOWCASE, false);
            editor.commit();
            showShiftsShowcase = false;
            showcase();
        }
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mListener = (OnFragmentInteractionListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    public interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        public void onFragmentInteraction(Uri uri);
    }

}
