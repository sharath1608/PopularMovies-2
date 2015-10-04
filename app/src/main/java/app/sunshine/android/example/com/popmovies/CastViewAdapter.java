package app.sunshine.android.example.com.popmovies;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.squareup.picasso.Callback;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Asus1 on 8/31/2015.
 */
public class CastViewAdapter extends RecyclerView.Adapter<CastViewAdapter.ViewHolder> {

    private List<CastViewObject> castViewList;
    private Context mContext;
    private OnItemClickListener mItemClickListener;

    public void setCastViewList(List<CastViewObject> castViewList) {
        this.castViewList = castViewList;
    }

    public CastViewAdapter(List<CastViewObject> castViewList){
        this.castViewList = new ArrayList<>(castViewList);
    }

    public interface OnItemClickListener{
        void onItemClick(View v, int position);
    }

    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener{
        public ImageView imageView;
        public TextView textView;
        public ViewHolder(View itemView) {
            super(itemView);
            imageView = (ImageView) itemView.findViewById(R.id.cast_image);
            textView = (TextView) itemView.findViewById(R.id.cast_name);
            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            if(mItemClickListener!=null){
                mItemClickListener.onItemClick(v,getPosition());
            }
        }
    }

    public void setOnItemClickListener(final OnItemClickListener listener){
        this.mItemClickListener = listener;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        mContext = parent.getContext();
        LayoutInflater layoutInflater = LayoutInflater.from(mContext);
        View castView = layoutInflater.inflate(R.layout.cast_item_layout,parent,false);
        ViewHolder viewHolder = new ViewHolder(castView);
        return  viewHolder;
    }

    @Override
    public void onBindViewHolder(CastViewAdapter.ViewHolder holder, int position) {
            final String LOG_TAG = getClass().getSimpleName();
            CastViewObject castObject = castViewList.get(position);
            TextView textView = holder.textView;
            textView.setText(castObject.getCastName());

        PicassoImageCache
                .getPicassoInstance(mContext)
                .load(castViewList.get(position).getCastImageUrl())
                .error(R.drawable.user_placeholder_image)
                .fit()
                .into(holder.imageView, new Callback() {
                    @Override
                    public void onSuccess() {
                    }

                    @Override
                    public void onError() {
                        Log.e(LOG_TAG, "Error in loading images");
                    }
                });
    }

    @Override
    public int getItemCount() {
        return (castViewList == null)? 0 : castViewList.size();
    }
}
