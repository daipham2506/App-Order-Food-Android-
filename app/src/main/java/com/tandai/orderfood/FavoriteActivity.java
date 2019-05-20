package com.tandai.orderfood;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.text.Layout;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.view.animation.LayoutAnimationController;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;

public class FavoriteActivity extends AppCompatActivity {

    DatabaseReference database;
    FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
    String userID = user.getUid();

    CoordinatorLayout coordinatorLayout;
    RecyclerView recyclerView;
    Favorite favorite;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.layout_favorite);

        initRecyclerView();


    }




    private void initRecyclerView(){
        recyclerView = (RecyclerView) findViewById(R.id.recycler_fav_food);
        coordinatorLayout = (CoordinatorLayout) findViewById(R.id.layout_favorite);


        recyclerView.setHasFixedSize(true);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this, LinearLayoutManager.VERTICAL,false);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        final ArrayList<Favorite> arrFavorite = new ArrayList<>();


        database = FirebaseDatabase.getInstance().getReference().child("Favorite").child(userID);
        database.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for(DataSnapshot ds : dataSnapshot.getChildren()){
                    if(dataSnapshot.getValue() != null) {
                        favorite = ds.getValue(Favorite.class);
                        if(favorite.getCheck() == 1)
                            arrFavorite.add(favorite);

                    }
                }
                final FavoriteAdapter favoriteAdapter = new FavoriteAdapter(arrFavorite,getApplicationContext());
                recyclerView.setAdapter(favoriteAdapter);


                //set animation
                runAnimation(recyclerView);

                //delete vs Undo Favorite food
                ItemTouchHelper.SimpleCallback simpleItemTouchCallback = new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT ) {

                    Drawable background;
                    Drawable xMark;
                    int xMarkMargin;
                    boolean initiated;

                    private void init() {
                        background = new ColorDrawable(Color.RED);
                        xMark = ContextCompat.getDrawable(FavoriteActivity.this, R.drawable.ic_delete_white_36);
                        xMark.setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_ATOP);
                        initiated = true;
                    }

                    @Override
                    public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
                        Toast.makeText(FavoriteActivity.this, "on Move", Toast.LENGTH_SHORT).show();
                        return false;
                    }

                    @Override
                    public void onSwiped(RecyclerView.ViewHolder viewHolder, int swipeDir) {

                        //Remove swiped item from list and notify the RecyclerView
                        final int position = viewHolder.getAdapterPosition();
                        final Favorite mRecentlyDeletedItem = arrFavorite.get(position);
                        final int mRecentlyDeletedItemPosition = position;
                        arrFavorite.remove(position);
                        favoriteAdapter.notifyDataSetChanged();
                        //database.child(mRecentlyDeletedItem.getFoodID()).child("check").setValue(0);


                        Snackbar snackbar = Snackbar
                                .make(coordinatorLayout, "Đã xóa "+mRecentlyDeletedItem.getFoodID(), Snackbar.LENGTH_LONG)
                                .setAction("Quay lại", new View.OnClickListener() {
                                    @Override
                                    public void onClick(View view) {
                                        Snackbar snackbar1 = Snackbar.make(coordinatorLayout, "Đã khôi phục "+mRecentlyDeletedItem.getFoodID(), Snackbar.LENGTH_LONG);
                                        arrFavorite.add(mRecentlyDeletedItemPosition,mRecentlyDeletedItem);
                                        favoriteAdapter.notifyDataSetChanged();
                                        snackbar1.show();
                                        //database.child(mRecentlyDeletedItem.getFoodID()).child("check").setValue(1);
                                    }
                                });

                        snackbar.show();

                    }

                    @Override
                    public void onChildDraw(Canvas c, RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, float dX, float dY, int actionState, boolean isCurrentlyActive) {
                        View itemView = viewHolder.itemView;

                        // not sure why, but this method get's called for viewholder that are already swiped away
                        if (viewHolder.getAdapterPosition() == -1) {
                            // not interested in those
                            return;
                        }

                        if (!initiated) {
                            init();
                        }

                        // draw red background
                        background.setBounds(itemView.getRight() + (int) dX, itemView.getTop(), itemView.getRight(), itemView.getBottom());
                        background.draw(c);

                        int xMarkLeft = 0;
                        int xMarkRight = 0;
                        int xMarkTop = itemView.getTop() + (itemView.getHeight() - xMark.getIntrinsicHeight()) / 2;
                        int xMarkBottom = xMarkTop + xMark.getIntrinsicHeight();
                        if (dX < 0) {
                            xMarkLeft = itemView.getRight() - xMarkMargin - xMark.getIntrinsicWidth();
                            xMarkRight = itemView.getRight() - xMarkMargin;
                        }
                        else {
                            xMarkLeft = itemView.getLeft() + xMarkMargin;
                            xMarkRight = itemView.getLeft() + xMarkMargin + xMark.getIntrinsicWidth();
                        }
                        xMark.setBounds(xMarkLeft, xMarkTop, xMarkRight, xMarkBottom);
                        xMark.draw(c);

                        super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
                    }

                };

                ItemTouchHelper itemTouchHelper = new ItemTouchHelper(simpleItemTouchCallback);
                itemTouchHelper.attachToRecyclerView(recyclerView);


            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

    }


    private void runAnimation(RecyclerView recyclerView) {
        LayoutAnimationController controller = null;

        controller = AnimationUtils.loadLayoutAnimation(recyclerView.getContext(),R.anim.layout_slide_from_left);

        recyclerView.setLayoutAnimation(controller);
        recyclerView.getAdapter().notifyDataSetChanged();
        recyclerView.scheduleLayoutAnimation();

    }


}