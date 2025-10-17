package com.example.voicenotereminder.ui

import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.RecyclerView

class SpaceItemDecoration(private val spacePx: Int) : RecyclerView.ItemDecoration() {
    override fun getItemOffsets(outRect: Rect, v: View, parent: RecyclerView, state: RecyclerView.State) {
        val pos = parent.getChildAdapterPosition(v)
        outRect.left = 0
        outRect.right = 0
        outRect.top = if (pos == 0) spacePx else spacePx / 2
        outRect.bottom = spacePx / 2
    }
}
