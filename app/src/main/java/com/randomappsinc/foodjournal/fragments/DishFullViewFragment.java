package com.randomappsinc.foodjournal.fragments;

import android.app.Fragment;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.joanzapata.iconify.IconDrawable;
import com.joanzapata.iconify.fonts.IoniconsIcons;
import com.randomappsinc.foodjournal.R;
import com.randomappsinc.foodjournal.models.Dish;
import com.randomappsinc.foodjournal.utils.Constants;
import com.randomappsinc.foodjournal.utils.UIUtils;
import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;

public class DishFullViewFragment extends Fragment {

    public static final String DISH_KEY = "dish";

    public static DishFullViewFragment newInstance(Dish dish, boolean fromRestaurant) {
        DishFullViewFragment fragment = new DishFullViewFragment();
        Bundle bundle = new Bundle();
        bundle.putParcelable(DISH_KEY, dish);
        bundle.putBoolean(Constants.FROM_RESTAURANT_KEY, fromRestaurant);
        fragment.setArguments(bundle);
        return fragment;
    }

    private final Callback mImageLoadingCallback = new Callback() {
        @Override
        public void onSuccess() {
            mParent.animate().alpha(1.0f).setDuration(getResources().getInteger(R.integer.default_anim_length));
        }

        @Override
        public void onError() {
            UIUtils.showToast(R.string.image_load_fail, Toast.LENGTH_LONG);
        }
    };

    @BindView(R.id.parent) View mParent;
    @BindView(R.id.picture) ImageView mPicture;
    @BindView(R.id.picture_label) TextView mPictureLabel;

    private Unbinder mUnbinder;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.dish_full_view, container, false);
        mUnbinder = ButterKnife.bind(this, rootView);

        Drawable defaultThumbnail = new IconDrawable(
                getActivity(),
                IoniconsIcons.ion_image).colorRes(R.color.dark_gray);
        Dish dish = getArguments().getParcelable(DISH_KEY);
        Picasso.with(getActivity())
                .load(dish.getUriString())
                .error(defaultThumbnail)
                .fit()
                .centerInside()
                .into(mPicture, mImageLoadingCallback);

        boolean fromRestaurant = getArguments().getBoolean(Constants.FROM_RESTAURANT_KEY);
        mPictureLabel.setMovementMethod(LinkMovementMethod.getInstance());
        mPictureLabel.setText(Html.fromHtml(dish.getDishInfoText(!fromRestaurant)));

        return rootView;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mUnbinder.unbind();
    }
}
