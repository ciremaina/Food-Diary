package com.randomappsinc.foodjournal.adapters;

import android.app.Activity;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.joanzapata.iconify.IconDrawable;
import com.joanzapata.iconify.fonts.IoniconsIcons;
import com.randomappsinc.foodjournal.R;
import com.randomappsinc.foodjournal.activities.DishesFullViewGalleryActivity;
import com.randomappsinc.foodjournal.models.Dish;
import com.randomappsinc.foodjournal.utils.Constants;
import com.randomappsinc.foodjournal.utils.DishUtils;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class DishGalleryAdapter extends RecyclerView.Adapter<DishGalleryAdapter.DishThumbnailViewHolder> {

    private Activity mActivity;
    private ArrayList<Dish> mDishes;
    private Drawable mDefaultThumbnail;

    public DishGalleryAdapter(Activity activity) {
        mActivity = activity;
        mDishes = new ArrayList<>();
        mDefaultThumbnail = new IconDrawable(activity, IoniconsIcons.ion_android_restaurant).colorRes(R.color.dark_gray);
    }

    public void setDishes(List<Dish> dishes) {
        mDishes.clear();
        mDishes.addAll(dishes);
        notifyDataSetChanged();
    }

    @Override
    public DishThumbnailViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(mActivity).inflate(R.layout.dish_gallery_cell, parent, false);
        return new DishThumbnailViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(DishThumbnailViewHolder holder, int position) {
        holder.loadDish(position);
    }

    @Override
    public int getItemCount() {
        return mDishes.size();
    }

    class DishThumbnailViewHolder extends RecyclerView.ViewHolder {

        @BindView(R.id.dish_picture) ImageView mDishPicture;

        DishThumbnailViewHolder(View view) {
            super(view);
            ButterKnife.bind(this, view);
        }

        void loadDish(int position) {
            Picasso.with(mActivity)
                    .load(mDishes.get(position).getUriString())
                    .error(mDefaultThumbnail)
                    .fit()
                    .centerCrop()
                    .into(mDishPicture);
        }

        @OnClick(R.id.dish_picture)
        void onPictureClicked() {
            Intent intent = new Intent(mActivity, DishesFullViewGalleryActivity.class);
            intent.putExtra(Constants.DISH_IDS_KEY, DishUtils.getDishIdList(mDishes));
            intent.putExtra(DishesFullViewGalleryActivity.POSITION_KEY, getAdapterPosition());
            mActivity.startActivity(intent);
            mActivity.overridePendingTransition(0, 0);
        }
    }
}
