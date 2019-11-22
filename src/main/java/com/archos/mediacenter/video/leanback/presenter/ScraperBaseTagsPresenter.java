// Copyright 2017 Archos SA
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.archos.mediacenter.video.leanback.presenter;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;

import androidx.core.content.ContextCompat;
import androidx.leanback.widget.ImageCardView;
import androidx.leanback.widget.Presenter;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.archos.mediacenter.video.R;
import com.archos.mediascraper.EpisodeTags;
import com.archos.mediascraper.MovieTags;
import com.archos.mediascraper.ScraperImage;
import com.archos.mediascraper.ShowTags;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;

/**
 * Presenter for for details about a scraper search result objects (SearchResult > BaseTags)
 * Created by vapillon on 04/05/15.
 */
public class ScraperBaseTagsPresenter extends Presenter {

    // TODO: this does not make use of the okhttp cache since it goes through picasso instead of ScraperImage

    private Context mContext;

    public class ViewHolder extends Presenter.ViewHolder {
        private ImageCardView mCardView;
        private PicassoImageCardViewTarget mImageCardViewTarget;
        private String mFallbackUrl;

        public ViewHolder(Context context) {
            super(new ImageCardView(context));
            mCardView = (ImageCardView)view;
            mCardView.setMainImageDimensions(getWidth(context), getHeight(context));
            mCardView.setMainImage(new ColorDrawable(ContextCompat.getColor(context, R.color.lb_basic_card_bg_color)));
            mCardView.setFocusable(true);
            mCardView.setFocusableInTouchMode(true);

            mImageCardViewTarget = new PicassoImageCardViewTarget(mCardView, this);
        }

        public int getWidth(Context context) {
            return context.getResources().getDimensionPixelSize(R.dimen.poster_width);
        }

        public int getHeight(Context context) {
            return context.getResources().getDimensionPixelSize(R.dimen.poster_height);
        }

        public ImageCardView getImageCardView() {
            return mCardView;
        }

        /**
         * non blocking (using Picasso to load the Uri)
         * @param posterUrl
         */
        protected void updateCardViewPoster(String posterUrl) {
            Picasso.get()
                    .load(posterUrl)
                    .resize(getWidth(mContext), getHeight(mContext))
                    .error(R.drawable.filetype_new_video)
                    .into(mImageCardViewTarget);
        }

        public void setFallbackUrl(String url) {
            mFallbackUrl = url;
        }
    }

    @Override
    public Presenter.ViewHolder onCreateViewHolder(ViewGroup parent) {
        mContext = parent.getContext();
        ViewHolder vh = new ViewHolder(parent.getContext());
        return vh;
    }

    @Override
    public void onBindViewHolder(Presenter.ViewHolder viewHolder, Object item) {
        ViewHolder vh = (ViewHolder)viewHolder;

        boolean foundImage = false;
        if (item instanceof MovieTags) {
            MovieTags tags = (MovieTags)item;
            vh.getImageCardView().setTitleText(tags.getTitle());

            String content = Integer.toString(tags.getYear());
            if (tags.getDirectorsFormatted()!=null) {
                content += (" - " + tags.getDirectorsFormatted());
            }
            vh.getImageCardView().setContentText(content);

            if (tags.getDefaultPoster()!=null) {
                vh.setFallbackUrl(tags.getDefaultPoster().getThumbUrl());
                vh.updateCardViewPoster(tags.getDefaultPoster().getLargeUrl());
                foundImage = true;
            }
        }
        else if (item instanceof EpisodeTags) {
            EpisodeTags tags = (EpisodeTags)item;
            vh.getImageCardView().setTitleText(tags.getShowTitle());

            ScraperImage episodePoster = tags.getDefaultPoster();
            if(episodePoster == null && tags.getShowTags() != null) {
                episodePoster = tags.getShowTags().getDefaultPoster();
            }
            if (episodePoster != null) {
                vh.setFallbackUrl(episodePoster.getThumbUrl());
                vh.updateCardViewPoster(episodePoster.getLargeUrl());
                foundImage = true;

            }
        }
        else if (item instanceof ShowTags) {
            ShowTags tags = (ShowTags)item;
            vh.getImageCardView().setTitleText(tags.getTitle());
            if (tags.getDefaultPoster()!=null) {
                vh.setFallbackUrl(tags.getDefaultPoster().getThumbUrl());
                vh.updateCardViewPoster(tags.getDefaultPoster().getLargeUrl());
                foundImage = true;
            }
        }
        else {
            throw new IllegalArgumentException("invalid object! "+item.getClass().getCanonicalName());
        }
        if(!foundImage) {
            vh.getImageCardView().setMainImageScaleType(ImageView.ScaleType.CENTER);
            vh.getImageCardView().setMainImage(mContext.getResources().getDrawable(R.drawable.filetype_new_video), true);
        }
    }

    @Override
    public void onUnbindViewHolder(Presenter.ViewHolder viewHolder) {
        ((ViewHolder)viewHolder).getImageCardView().setMainImage(null);
    }

    public class PicassoImageCardViewTarget implements Target {
        private ImageCardView mImageCardView;
        private ViewHolder mViewHolder;

        // Picasso documentation: Objects implementing this class must have a working implementation of Object.equals(Object) and Object.hashCode() for proper storage internally.
        @Override
        public boolean equals(Object other) {
            if (other==null || !(other instanceof PicassoImageCardViewTarget)) {
                return false;
            }
            // mImageCardView must never be null, no need to check it!
            return mImageCardView.equals( ((PicassoImageCardViewTarget)other).mImageCardView );
        }

        public PicassoImageCardViewTarget(ImageCardView imageCardView, ViewHolder viewHolder) {
            mImageCardView = imageCardView;
            mViewHolder = viewHolder;
        }

        @Override
        public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom loadedFrom) {
            Drawable bitmapDrawable = new BitmapDrawable(mContext.getResources(), bitmap);
            mImageCardView.setMainImageScaleType(ImageView.ScaleType.CENTER_CROP);

            // Do not fade-in when loading from cache memory (because it is most likely instantaneous in that case)
            boolean fade = (loadedFrom!= Picasso.LoadedFrom.MEMORY);
            mImageCardView.setMainImage(bitmapDrawable, fade);
        }

        @Override
        public void onBitmapFailed(Exception e, Drawable drawable){
            String fallback = mViewHolder.mFallbackUrl;
            if(fallback != null) {
                mViewHolder.setFallbackUrl(null);
                mViewHolder.updateCardViewPoster(fallback);
                return;
            }
            mImageCardView.setMainImageScaleType(ImageView.ScaleType.CENTER);
            mImageCardView.setMainImage(drawable, true);
        }

        @Override
        public void onPrepareLoad(Drawable drawable) {
            // Do nothing
        }
    }
}
