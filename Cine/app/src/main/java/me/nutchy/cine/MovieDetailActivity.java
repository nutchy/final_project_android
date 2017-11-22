package me.nutchy.cine;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.support.v7.widget.Toolbar;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.GenericTypeIndicator;
import com.google.firebase.database.ValueEventListener;

import butterknife.ButterKnife;
import me.nutchy.cine.Adapter.CommentsAdapter;
import me.nutchy.cine.Model.Comment;
import me.nutchy.cine.Model.FavoriteMovie;
import me.nutchy.cine.Model.Movie;
import me.nutchy.cine.Model.Rating;

public class MovieDetailActivity extends AppCompatActivity {
    private DatabaseReference databaseReference;
    private Movie movie;
    private FavoriteMovie favoriteMovie;
    private FirebaseUser user;
    private Context context = this;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_movie_detail);
        ButterKnife.bind(this);
        Intent intent = getIntent();
        movie = intent.getParcelableExtra("movie");
        user = FirebaseAuth.getInstance().getCurrentUser();
        favoriteMovie = new FavoriteMovie();
        databaseReference = FirebaseDatabase.getInstance().getReference();
        initToolbar();
        initRatingBar();
        initComment();
        initFavorite();
        displayPoster();
        displayComment();
    }

    private void initFavorite() {
        final Button btn_favorite = (Button) findViewById(R.id.btn_favorite);
        DatabaseReference mFavoriteRef = databaseReference.child("favorites")
                .child(String.valueOf(user.getUid()));

        mFavoriteRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                for (DataSnapshot ds : dataSnapshot.getChildren()) {
                    if (ds.getValue().equals(movie.getId())) {
                        favoriteMovie = new FavoriteMovie(String.valueOf(movie.getId()));
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
        btn_favorite.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onClickFavorite();
            }
        });

    }

    public void onClickFavorite() {
        DatabaseReference mFavoriteRef = databaseReference.child("favorites")
                .child(String.valueOf(user.getUid()));
        if (favoriteMovie != null && favoriteMovie.getMovieId() != null) {
            // Remove Favorite
            favoriteMovie = new FavoriteMovie();
            mFavoriteRef.setValue(favoriteMovie);
            Toast.makeText(MovieDetailActivity.this, "Deleted.", Toast.LENGTH_SHORT).show();
        } else {
            // Add this movie to favorite
            favoriteMovie = new FavoriteMovie(String.valueOf(movie.getId()));
            mFavoriteRef.setValue(favoriteMovie);
            Toast.makeText(MovieDetailActivity.this, "Added to Favorite.", Toast.LENGTH_SHORT).show();
        }
    }

    private void initComment() {
        final EditText et_comment = (EditText) findViewById(R.id.et_comment);
        Button btn_comment = (Button) findViewById(R.id.btn_comment);
        btn_comment.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String comment = et_comment.getText().toString();
                addCommentToFirebase(comment);
            }
        });
    }

    private void initRatingBar() {
        Button ratingBtn = (Button) findViewById(R.id.btn_rating);
        final TextView ratingTv = (TextView) findViewById(R.id.tv_rating);
        RatingBar ratingBar = (RatingBar) findViewById(R.id.rating_bar);
        ratingBar.setOnRatingBarChangeListener(new RatingBar.OnRatingBarChangeListener() {
            @Override
            public void onRatingChanged(RatingBar ratingBar, float rating, boolean fromUser) {
                ratingTv.setText(String.valueOf((int) rating));
            }
        });
        ratingBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int rating = Integer.parseInt(ratingTv.getText().toString());
                addRatingToFirebase(rating);
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(),
                        InputMethodManager.HIDE_NOT_ALWAYS);

            }
        });
    }

    private void addRatingToFirebase(int rating) {
        DatabaseReference mRatingRef = databaseReference
                .child("ratings");
        String key = mRatingRef.child(String.valueOf(movie.getId())).push().getKey();
        Rating mRating = new Rating(user.getUid(), movie.getId(), rating);
        mRatingRef.child(String.valueOf(movie.getId())).child(key).setValue(mRating);

        Toast.makeText(MovieDetailActivity.this
                , String.valueOf(rating), Toast.LENGTH_SHORT).show();
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    public void displayPoster() {
        ImageView iV_poster = (ImageView) findViewById(R.id.iV_poster);
        Glide.with(this).load(movie.BASE_URL_POSTER + movie.getPoster_path())
                .into(iV_poster);
    }

    public void displayComment() {
        CommentsAdapter commentsAdapter = new CommentsAdapter(this, movie);
        RecyclerView recyclerView = (RecyclerView) findViewById(R.id.rc_comments);
        recyclerView.setLayoutManager(new LinearLayoutManager
                (this, LinearLayoutManager.VERTICAL, false));
        recyclerView.setAdapter(commentsAdapter);
    }

    public void initToolbar() {
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }
        toolbar.setTitle(movie.getTitle());
    }

    public void addCommentToFirebase(String comment) {
        DatabaseReference mCommentRef = databaseReference.child("comments");
        String key = mCommentRef.child(String.valueOf(movie.getId())).push().getKey();
        Comment mComment = new Comment(
                comment, user.getDisplayName(), user.getUid(), movie.getId(), key);
        mCommentRef.child(String.valueOf(movie.getId())).child(key).setValue(mComment);
        Toast.makeText(MovieDetailActivity.this, "Comment Added.", Toast.LENGTH_SHORT)
                .show();
    }


}
