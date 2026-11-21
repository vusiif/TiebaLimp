package com.huanchengfly.tieba.post.activities

import android.view.View
import androidx.coordinatorlayout.widget.CoordinatorLayout
import com.google.android.material.bottomsheet.BottomSheetBehavior

object TranslucentBottomSheetHelper {
    fun setupBottomSheet(
        bottomSheet: View,
        maskView: View
    ): BottomSheetBehavior<View> {
        val behavior =
            (bottomSheet.layoutParams as CoordinatorLayout.LayoutParams).behavior as BottomSheetBehavior<View>
        behavior.addBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
            override fun onStateChanged(bottomSheet: View, newState: Int) {}

            override fun onSlide(bottomSheet: View, slideOffset: Float) {
                maskView.alpha = slideOffset
                maskView.visibility = if (slideOffset < 0.01f) {
                    View.GONE
                } else {
                    View.VISIBLE
                }
            }
        })
        maskView.setOnClickListener {
            behavior.state = BottomSheetBehavior.STATE_COLLAPSED
        }
        return behavior
    }
}
