package com.mikerinehart.rideguide.main_fragments;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Point;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.NumberPicker;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.materialdialogs.MaterialDialog;
import com.appyvet.rangebar.RangeBar;
import com.gc.materialdesign.views.ProgressBarCircularIndeterminate;
import com.gc.materialdesign.views.Slider;
import com.github.amlcurran.showcaseview.ShowcaseView;
import com.github.amlcurran.showcaseview.targets.Target;
import com.github.amlcurran.showcaseview.targets.ViewTarget;
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
import com.mikerinehart.rideguide.adapters.AvailableDriversAdapter;
import com.mikerinehart.rideguide.adapters.AvailableRidesTimeSlotsAdapter;
import com.mikerinehart.rideguide.models.Reservation;
import com.mikerinehart.rideguide.models.Shift;
import com.mikerinehart.rideguide.models.User;
import com.timehop.stickyheadersrecyclerview.StickyRecyclerHeadersDecoration;

import org.apache.http.Header;
import org.json.JSONArray;
import org.json.JSONObject;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import se.walkercrou.places.GooglePlaces;
import se.walkercrou.places.Place;


public class RidesFragment extends Fragment {
    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private User me;
    private String mParam2;

    private OnFragmentInteractionListener mListener;

    private TextView noRides;
    private ProgressBarCircularIndeterminate loadingIcon;
    SwipeRefreshLayout mSwipeRefreshLayout;
    private RecyclerView ridesList;

    AvailableRidesTimeSlotsAdapter adapter;
    AutoCompleteTextView origin;
    AutoCompleteTextView destination;

    ArrayAdapter<Place> placesAdapter;
    int threadId = 0;
    GooglePlaces client;

    SharedPreferences sp;
    private boolean showRidesShowcase;

    private String TAG = "AvailableRidesPageFragment";

    public RidesFragment() {
        // Required empty public constructor
    }

    public static RidesFragment newInstance(User param1, String param2) {
        RidesFragment fragment = new RidesFragment();
        Bundle args = new Bundle();
        args.putParcelable("USER", param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        MainActivity.drawerAdapter.selectPosition(2);
        if (getArguments() != null) {
            me = getArguments().getParcelable("USER");
            mParam2 = getArguments().getString(ARG_PARAM2);
        }
        sp = getActivity().getSharedPreferences(Constants.PREFERENCES, Context.MODE_PRIVATE);
        showRidesShowcase = sp.getBoolean(Constants.SHOWRIDESSHOWCASE, true); // True if need to show
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        MainActivity.toolbar.setTitle("Available Rides");
        client = new GooglePlaces("AIzaSyCfg_jCCi8boOcSSzMuPY0TAVhHKn7W2X4");

        final View v = inflater.inflate(R.layout.fragment_rides_available_page, container, false);

        noRides = (TextView)v.findViewById(R.id.rides_available_none);
        loadingIcon = (ProgressBarCircularIndeterminate)v.findViewById(R.id.rides_available_circular_loading);
        mSwipeRefreshLayout = (SwipeRefreshLayout)v.findViewById(R.id.rides_available_swipe_refresh_layout);
        mSwipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                getActivity().getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.container, RidesFragment.newInstance(me, "RidesFragment"))
                        .commit();
                //refreshContent();
            }
        });
        ridesList = (RecyclerView)v.findViewById(R.id.rides_available_list);
        LinearLayoutManager llm = new LinearLayoutManager(ridesList.getContext());
        llm.setOrientation(LinearLayoutManager.VERTICAL);
        ridesList.setLayoutManager(llm);

        refreshContent();

        final GestureDetector mGestureDetector = new GestureDetector(getActivity(), new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onSingleTapUp(MotionEvent e) {
                return true;
            }
        });

        ridesList.addOnItemTouchListener(new RecyclerView.OnItemTouchListener() {
            @Override
            public boolean onInterceptTouchEvent(RecyclerView recyclerView, MotionEvent motionEvent) {
                ViewGroup child = (ViewGroup) recyclerView.findChildViewUnder(motionEvent.getX(), motionEvent.getY());

                if (child != null && mGestureDetector.onTouchEvent(motionEvent)) {
                    int itemClicked = recyclerView.getChildPosition(child);
                    List<Reservation> driverList = adapter.getDrivers(itemClicked);
                    createAvailableDriversDialog(driverList);
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

    private void createAvailableDriversDialog(List<Reservation> driverList) {
        final LayoutInflater inflater = LayoutInflater.from(getActivity().getBaseContext());
        View dialogLayout = inflater.inflate(R.layout.availablerides_view_drivers_dialog, null);
        final MaterialDialog chooseDriverDialog = new MaterialDialog.Builder(RidesFragment.this.getActivity())
                .title("Pick a Driver")
                .customView(dialogLayout)
                .positiveText("Cancel")
                .build();
        final RecyclerView availableDriversList = (RecyclerView)dialogLayout.findViewById(R.id.rides_available_view_drivers_dialog_list);
        availableDriversList.addItemDecoration(new SimpleDividerItemDecoration(availableDriversList.getContext()));

        LinearLayoutManager llm = new LinearLayoutManager(availableDriversList.getContext());
        llm.setOrientation(LinearLayoutManager.VERTICAL);
        availableDriversList.setLayoutManager(llm);

        final AvailableDriversAdapter adapter = new AvailableDriversAdapter(driverList);
        availableDriversList.setAdapter(adapter);
        chooseDriverDialog.show();

        final GestureDetector mGestureDetector = new GestureDetector(getActivity(), new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onSingleTapUp(MotionEvent e) {
                return true;
            }
        });
        availableDriversList.addOnItemTouchListener(new RecyclerView.OnItemTouchListener() {
            @Override
            public boolean onInterceptTouchEvent(RecyclerView recyclerView, MotionEvent motionEvent) {
                ViewGroup child = (ViewGroup) recyclerView.findChildViewUnder(motionEvent.getX(), motionEvent.getY());

                if (child != null && mGestureDetector.onTouchEvent(motionEvent)) {
                    int itemClicked = recyclerView.getChildPosition(child);
                    final Reservation r = adapter.getReservation(itemClicked);
                    final View dialogLayout = inflater.inflate(R.layout.rides_available_make_reservation_dialog, null);
                    final RangeBar np = (RangeBar)dialogLayout.findViewById(R.id.rides_available_make_reservation_dialog_number_slider);
                    final EditText numPassengers = (EditText)dialogLayout.findViewById(R.id.rides_available_make_reservation_dialog_numpassengers);
                    numPassengers.setText("1");

                    np.setTickStart(1);
                    np.setTickEnd(r.getShift().getSeats());
                    np.setTickInterval(1);
                    np.setRangePinsByIndices(0, 0);
                    np.setConnectingLineWeight(7);

                    np.setOnRangeBarChangeListener(new RangeBar.OnRangeBarChangeListener() {
                        @Override
                        public void onRangeChangeListener(RangeBar rangebar, int leftPinIndex, int rightPinIndex, String leftPinValue, String rightPinValue) {
                            numPassengers.setText(rightPinValue);
                        }
                    });

                    destination = (AutoCompleteTextView)dialogLayout.findViewById(R.id.rides_available_make_reservation_dialog_destination);
                    origin = (AutoCompleteTextView)dialogLayout.findViewById(R.id.rides_available_make_reservation_dialog_origin);

                            origin.addTextChangedListener(new TextWatcher() {
                                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                                }

                                public void onTextChanged(CharSequence s, int start, int before, int count) {
                                }

                                public void afterTextChanged(final Editable s) {
                                    threadId++;
                                    Thread thread = new Thread(new PlacesSearchRunnableJob(true, s.toString()), Integer.toString(threadId));
                                    thread.start();
                                }
                            });
                            destination.addTextChangedListener(new TextWatcher() {
                                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                                }

                                public void onTextChanged(CharSequence s, int start, int before, int count) {
                                }

                                public void afterTextChanged(final Editable s) {
                                    threadId++;
                                    Thread thread = new Thread(new PlacesSearchRunnableJob(false, s.toString()), Integer.toString(threadId));
                                    thread.start();
                                }
                            });

                    final MaterialDialog createReservationDialog = new MaterialDialog.Builder(RidesFragment.this.getActivity())
                            .title("Create Reservation")
                            .customView(dialogLayout)
                            .negativeText("Cancel")
                            .positiveText("Create")
                            .callback(new MaterialDialog.ButtonCallback() {
                                @Override
                                public void onPositive(MaterialDialog dialog) {
                                    RequestParams params = new RequestParams("user_id", me.getId());
                                    params.put("reservation_id", r.getId());
                                    params.put("origin", origin.getText());
                                    params.put("destination", destination.getText());
                                    params.put("passengers", numPassengers.getText().toString());
                                    RestClient.post("reservations/makeReservation", params, new JsonHttpResponseHandler() {
                                        @Override
                                        public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                                            chooseDriverDialog.dismiss();
                                            getActivity().getSupportFragmentManager()
                                                    .beginTransaction()
                                                    .replace(R.id.container, RidesFragment.newInstance(me, "RidesFragment"))
                                                    .commit();
                                            Toast.makeText(getActivity().getApplicationContext(), "Reservation successful!", Toast.LENGTH_SHORT).show();
                                            Log.i(TAG, "Reservaiton successful!");
                                        }

                                        @Override
                                        public void onFailure(int statusCode, Header[] headers, String response, Throwable throwable) {
                                            Log.i(TAG, "Error " + statusCode + ": " + response);
                                            Toast.makeText(getActivity().getApplicationContext(), "Error, please try again", Toast.LENGTH_SHORT).show();
                                        }
                                    });
                                }
                            })
                            .build();
                    createReservationDialog.show();
                }
                return false;
            }

            @Override
            public void onTouchEvent(RecyclerView recyclerView, MotionEvent motionEvent) {
                Log.i(TAG, "TouchEvent");
            }
        });
    }

    private void refreshContent() {
            RestClient.post("reservations/freeReservations", null, new JsonHttpResponseHandler() {
                @Override
                public void onSuccess(int statusCode, Header[] headers, JSONArray response) {
                    List<Reservation> result;
                    Gson gson = new GsonBuilder().setDateFormat("yyyy-MM-dd HH:mm:ss").create();
                    Type listType = new TypeToken<List<Reservation>>() {
                    }.getType();

                    result = (List<Reservation>) gson.fromJson(response.toString(), listType);

                    // check whether or not any rides even exist
                    if (result.size() == 0) {
                        noRides.setVisibility(TextView.VISIBLE);
                        loadingIcon.setVisibility(ProgressBarCircularIndeterminate.GONE);
                    } else {
                        noRides.setVisibility(TextView.GONE);
                        ridesList.setVisibility(RecyclerView.VISIBLE);

                        List<List<Reservation>> mainList = new ArrayList<List<Reservation>>();
                        for (int i = 0; i < result.size(); i++) {
                            if (mainList.isEmpty()) {
                                List<Reservation> newList = new ArrayList<Reservation>();
                                newList.add(result.get(i));
                                mainList.add(newList);
                            } else {
                                boolean added = false;

                                for (List<Reservation> x : mainList) {
                                    if (x.get(0).getPickup_time().compareTo(result.get(i).getPickup_time()) == 0) {
                                        x.add(result.get(i));
                                        added = true;
                                        break;
                                    }
                                }
                                if (!added) {
                                    List<Reservation> newList = new ArrayList<Reservation>();
                                    newList.add(result.get(i));
                                    mainList.add(newList);
                                }
                            }
                        }

                        adapter = new AvailableRidesTimeSlotsAdapter(mainList);

                        ridesList.addItemDecoration(new SimpleDividerItemDecoration(ridesList.getContext()));

                        loadingIcon.setVisibility(ProgressBarCircularIndeterminate.GONE);
                        ridesList.setAdapter(adapter);
                        StickyRecyclerHeadersDecoration headersDecor = new StickyRecyclerHeadersDecoration(adapter);
                        ridesList.addItemDecoration(headersDecor);
                    }
                    mSwipeRefreshLayout.setRefreshing(false);
                }

                @Override
                public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONObject errorResponse) {
                    Log.i(TAG, "Error: " + errorResponse);
                    mSwipeRefreshLayout.setRefreshing(false);
                }
            });
    }

    private void showcase() {
        ShowcaseView sv = new ShowcaseView.Builder(getActivity(), true)
                .setContentTitle("Find a Ride")
                .setContentText("Designated drivers with open timeslots will \nshow up here with times available for you\n to be picked up." +
                        " Clicking a timeslot\n will show available drivers.")
                .hideOnTouchOutside()
                .setStyle(R.style.CustomShowcaseTheme2)
                .build();
    }

    public void onResume() {
        super.onResume();
        if (showRidesShowcase) {
            SharedPreferences.Editor editor = sp.edit();
            editor.putBoolean(Constants.SHOWRIDESSHOWCASE, false);
            editor.commit();
            showRidesShowcase = false;
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

    class PlacesSearchRunnableJob implements Runnable {
        String search;
        boolean entryField; // true = origin, false = destination

        public PlacesSearchRunnableJob(boolean field, String s) {

            search = s;
            entryField = field;
        }

        public void run() {
            Thread t = Thread.currentThread();
            try {
                t.sleep(400);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            if (t.getName().equals(Integer.toString(threadId)) && !search.isEmpty()) {
                Log.i(TAG, "Timer expired, searching!");
                final List<Place> places;
                try {
                    places = client.getPlacesByQuery(search, GooglePlaces.DEFAULT_RESULTS);
                    placesAdapter = new ArrayAdapter<Place>(getActivity().getBaseContext(), android.R.layout.simple_list_item_1, places) {
                        @Override
                        public View getView(int position, View convertView, ViewGroup parent) {
                            View view = super.getView(position, convertView, parent);
                            TextView name = (TextView) view.findViewById(android.R.id.text1);
                            name.setText(places.get(position).getName() + "\n" + places.get(position).getAddress());
                            return view;
                        }
                    };
                    getActivity().runOnUiThread(new Runnable() {
                        public void run() {
                            if (entryField) {
                                placesAdapter.notifyDataSetChanged();
                                origin.setAdapter(placesAdapter);
                                placesAdapter.notifyDataSetChanged();
                                origin.setOnItemClickListener( new AdapterView.OnItemClickListener() {
                                    @Override
                                    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                                        origin.setText(placesAdapter.getItem(position).getName());
                                    }
                                });
                            } else {
                                destination.setAdapter(placesAdapter);
                                placesAdapter.notifyDataSetChanged();
                                destination.setOnItemClickListener( new AdapterView.OnItemClickListener() {
                                    @Override
                                    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                                        destination.setText(placesAdapter.getItem(position).getName());
                                    }
                                });
                            }

                        }
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                }

            }

        }
    }

}
