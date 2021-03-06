package com.randomappsinc.foodjournal.views;

import android.view.View;
import android.widget.TextView;

import com.randomappsinc.foodjournal.R;

import java.util.List;

import butterknife.BindColor;
import butterknife.BindViews;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class RatingView {

    @BindViews({R.id.star_1, R.id.star_2, R.id.star_3, R.id.star_4, R.id.star_5})
    List<TextView> mStars;
    @BindColor(R.color.dark_gray) int mDarkGray;
    @BindColor(R.color.app_red) int mRed;

    private int mRating;

    public RatingView(View parent) {
        ButterKnife.bind(this, parent);
        mRating = 0;
    }

    @OnClick({R.id.star_1, R.id.star_2, R.id.star_3, R.id.star_4, R.id.star_5})
    public void chooseRating(View clickedStar) {
        switch (clickedStar.getId()) {
            case R.id.star_1:
                loadRating(1);
                break;
            case R.id.star_2:
                loadRating(2);
                break;
            case R.id.star_3:
                loadRating(3);
                break;
            case R.id.star_4:
                loadRating(4);
                break;
            case R.id.star_5:
                loadRating(5);
                break;
        }
    }

    public void loadRating(int rating) {
        mRating = rating;

        for (int i = 0; i < mStars.size(); i++) {
            TextView currentStar = mStars.get(i);
            if (i < rating) {
                currentStar.setText(R.string.filled_star);
                currentStar.setTextColor(mRed);
            } else {
                currentStar.setText(R.string.blank_star);
                currentStar.setTextColor(mDarkGray);
            }
        }
    }

    public int getRating() {
        return mRating;
    }
}
