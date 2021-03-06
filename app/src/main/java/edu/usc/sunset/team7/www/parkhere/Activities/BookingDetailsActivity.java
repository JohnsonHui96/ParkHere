package edu.usc.sunset.team7.www.parkhere.Activities;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatButton;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import edu.usc.sunset.team7.www.parkhere.R;
import edu.usc.sunset.team7.www.parkhere.Utils.Consts;
import edu.usc.sunset.team7.www.parkhere.Utils.Tools;
import edu.usc.sunset.team7.www.parkhere.objectmodule.Booking;
import edu.usc.sunset.team7.www.parkhere.objectmodule.Listing;

/**
 * Created by Acer on 10/30/2016.
 */

public class BookingDetailsActivity extends AppCompatActivity {

    @BindView(R.id.booking_name) TextView bookingName;
    @BindView(R.id.provider_name) TextView providerName;
    @BindView(R.id.parking_image) ImageView parkingImage;
    @BindView(R.id.booking_details) TextView bookingDetails;
    @BindView(R.id.review_booking_button) AppCompatButton reviewBookingButton;
    @BindView(R.id.cancel_booking_button) AppCompatButton cancelBookingButton;
    @BindView(R.id.booking_details_toolbar) Toolbar bookingDetailsToolbar;

    private static final String TAG = "BookingDetailsActivity";
    private Booking booking;
    private Listing listing;
    private String providerFirstName;
    private String seekerFirstName;
    private String providerPhoneNumber;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_booking_details);
        ButterKnife.bind(this);

        setSupportActionBar(bookingDetailsToolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Booking details");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        booking = (Booking) getIntent().getSerializableExtra(Consts.BOOKING_EXTRA);
        if(booking != null) {
            listing = booking.getMListing();
            getFirebaseData();
        } else {
            Log.d(TAG, "BOOKING OBJECT IS EMPTY!");
        }
    }

    public static void startActivity(Context context, Booking booking) {
        System.out.println("Started activity");
        Intent intent = new Intent(context, BookingDetailsActivity.class);
        intent.putExtra(Consts.BOOKING_EXTRA, booking);
        context.startActivity(intent);
    }

    private void getFirebaseData() {
        providerFirstName = null;
        providerPhoneNumber = null;

        //getting first name of provider
        DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference(Consts.USERS_DATABASE)
                .child(listing.getProviderID());
        dbRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                getFirstNameAndPhoneNumber(dataSnapshot);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.w(TAG, "loadProviderName:onCancelled", databaseError.toException());
            }
        });
    }

    private void getFirstNameAndPhoneNumber(DataSnapshot userData) {
        providerFirstName = userData.child(Consts.USER_FIRSTNAME).getValue().toString();
        providerPhoneNumber = userData.child(Consts.USER_PHONENUMBER).getValue().toString();
        displayBooking();
    }

    private void displayBooking() {
        if(!listing.isRefundable()){
            cancelBookingButton.setEnabled(false);
        }
        bookingName.setText(listing.getName());
        providerName.setText(providerFirstName);
        Picasso.with(this).load(listing.getImageURL()).into(parkingImage);
        bookingDetails.setText(bookingDetailsString());
    }

    private String bookingDetailsString() {
        StringBuilder descriptionBuilder = new StringBuilder();
        descriptionBuilder.append("Name of Listing: " + listing.getName());
        descriptionBuilder.append("\nListing Description: "  + listing.getDescription());
        descriptionBuilder.append("\nStart Time: " + Tools.convertUnixTimeToDateString(listing.getStartTime()));
        descriptionBuilder.append("\nEnd Time: " + Tools.convertUnixTimeToDateString(listing.getStopTime()));
        descriptionBuilder.append("\nListing provider: " + providerFirstName);
        descriptionBuilder.append("\nProvider phone number: " + providerPhoneNumber);

        descriptionBuilder.append("\n\nParking Information");
        descriptionBuilder.append("\nLocation: (" + listing.getLatitude() + "," + listing.getLongitude() + ")");
        descriptionBuilder.append("\nPrice: " + listing.getPrice());
        descriptionBuilder.append("\nRefundable? " +listing.isRefundable());
        descriptionBuilder.append("\nHandicap? " + listing.isHandicap());
        descriptionBuilder.append("\nCovered? " + listing.isCovered());
        descriptionBuilder.append("\nCompact? " + listing.isCompact());

        return descriptionBuilder.toString();
    }

    @OnClick(R.id.provider_name)
    protected void displayProvider(){
        //Go to public user profile activity
        Intent intent = new Intent(BookingDetailsActivity.this, UserProfileActivity.class);
        Bundle bundle = new Bundle();
        bundle.putString(Consts.USER_ID, listing.getProviderID());
        intent.putExtras(bundle);
        startActivity(intent);
    }

    @OnClick(R.id.review_booking_button)
    protected void reviewBooking() {
        Intent intent = new Intent(BookingDetailsActivity.this, ReviewActivity.class);
        Bundle bundle = new Bundle();
        bundle.putSerializable(Consts.BOOKING_EXTRA, booking);
        intent.putExtras(bundle);
        startActivity(intent);
    }

    @OnClick(R.id.cancel_booking_button)
    protected void cancelBooking() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference().child(Consts.BOOKINGS_DATABASE).child(currentUser.getUid());
        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                for(DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    if(snapshot.getKey().toString().equals(booking.getBookingID())) {
                        String startTime = snapshot.child(Consts.BOOKING_START_TIME).getValue().toString();
                        String providerID = snapshot.child(Consts.BOOKING_PROVIDER_ID).getValue().toString();
                        String listingID = snapshot.child(Consts.BOOKING_LISTING_ID).getValue().toString();
                        long longStartTime = Long.parseLong(startTime);
                        long unixTime = System.currentTimeMillis() / 1000L;
                        if(unixTime < longStartTime) {
                            removeListing(listingID, providerID, booking.getBookingID());
                        } else {
                            AlertDialog.Builder adb=new AlertDialog.Builder(BookingDetailsActivity.this);
                            adb.setTitle("This booking cannot be cancelled because the transaction has been completed");
                            adb.setPositiveButton("OK", null);
                            adb.show();
                        }
                    }
                }
                HomeActivity.startActivityPostBooking(BookingDetailsActivity.this);
                finish();
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {}
        });

       // DatabaseReference providerListingRef = FirebaseDatabase.getInstance().getReference().child(Consts.LISTINGS_DATABASE).
    }

    protected void removeListing(final String listingID, final String providerID, final String bookingID){
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference().child(Consts.LISTINGS_DATABASE).child(providerID).child(Consts.INACTIVE_LISTINGS).child(listingID);
        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                System.out.println(dataSnapshot.getKey());
                DataSnapshot snap = dataSnapshot.child(Consts.LISTING_REFUNDABLE);
                boolean isRefundable = Boolean.parseBoolean(snap.getValue().toString());
                Listing listing = new Listing();
                if(isRefundable) {
                    final DatabaseReference addToActive =  FirebaseDatabase.getInstance().getReference().child(Consts.LISTINGS_DATABASE).child(providerID).child(Consts.ACTIVE_LISTINGS).child(listingID);
                    for(DataSnapshot child : dataSnapshot.getChildren()){
                        //add back to active listing
                        switch (child.getKey()) {
                            case "Compact":
                                addToActive.child(Consts.LISTING_COMPACT).setValue(child.getValue());
                                break;
                            case "Covered":
                                addToActive.child(Consts.LISTING_COVERED).setValue(child.getValue());
                                break;
                            case "Listing Description":
                                addToActive.child(Consts.LISTING_DESCRIPTION).setValue(child.getValue());
                                break;
                            case "Handicap":
                                addToActive.child(Consts.LISTING_HANDICAP).setValue(child.getValue());
                                break;
                            case "Image URL":
                                addToActive.child(Consts.LISTING_IMAGE).setValue(child.getValue());
                                break;
                            case "Latitude":
                                addToActive.child(Consts.LISTING_LATITUDE).setValue(child.getValue());
                                break;
                            case "Longitude":
                                addToActive.child(Consts.LISTING_LONGITUDE).setValue(child.getValue());
                                break;
                            case "Listing Name":
                                addToActive.child(Consts.LISTING_NAME).setValue(child.getValue());
                                break;
                            case "Is Refundable":
                                addToActive.child(Consts.LISTING_REFUNDABLE).setValue(child.getValue());
                                break;
                            case "Start Time":
                                addToActive.child(Consts.LISTING_START_TIME).setValue(child.getValue());
                                break;
                            case "End Time":
                                addToActive.child(Consts.LISTING_END_TIME).setValue(child.getValue());
                                break;
                            case "Price":
                                addToActive.child(Consts.LISTING_PRICE).setValue(child.getValue());
                                break;
                        }
                    }

                }
                //remove from inactive listing
                FirebaseDatabase.getInstance().getReference().child(Consts.LISTINGS_DATABASE).child(providerID).child(Consts.INACTIVE_LISTINGS).child(listingID).removeValue();
                FirebaseDatabase.getInstance().getReference().child(Consts.BOOKINGS_DATABASE).child(FirebaseAuth.getInstance().getCurrentUser().getUid()).child(bookingID).removeValue();
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }
}
