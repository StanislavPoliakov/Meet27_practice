package home.stanislavpoliakov.meet27_practice;

import android.graphics.Bitmap;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;

public class DiffCallback extends DiffUtil.ItemCallback<Bitmap> {

    @Override
    public boolean areItemsTheSame(@NonNull Bitmap oldItem, @NonNull Bitmap newItem) {
        return false;
    }

    @Override
    public boolean areContentsTheSame(@NonNull Bitmap oldItem, @NonNull Bitmap newItem) {
        return false;
    }
}
