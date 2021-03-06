package com.twoeightnine.root.xvii.chats.attachments.photos

import android.content.Context
import android.view.View
import com.twoeightnine.root.xvii.R
import com.twoeightnine.root.xvii.chats.attachments.base.BaseAttachmentsAdapter
import com.twoeightnine.root.xvii.model.attachments.Photo
import com.twoeightnine.root.xvii.utils.load
import com.twoeightnine.root.xvii.utils.setVisible
import kotlinx.android.synthetic.main.item_photo_attachment.view.*

class PhotoAttachmentsAdapter(
        context: Context,
        loader: (Int) -> Unit,
        private val onClick: (Photo) -> Unit
) : BaseAttachmentsAdapter<Photo, PhotoAttachmentsAdapter.PhotoViewHolder>(context, loader) {

    override fun getLayoutId() = R.layout.item_photo_attachment

    override fun getViewHolder(view: View) = PhotoViewHolder(view)

    override fun createStubLoadItem() = Photo()

    inner class PhotoViewHolder(view: View) : BaseAttachmentViewHolder<Photo>(view) {

        override fun bind(item: Photo) {
            with(itemView) {
                ivCheck.setVisible(item in multiSelect)
                ivPhoto.load(item.optimalPhoto)
                ivPhoto.setOnClickListener {
                    if (multiSelectMode) {
                        val i = items[adapterPosition]
                        multiSelect(i)
                        ivCheck.setVisible(i in multiSelect)
                    } else {
                        onClick(items[adapterPosition])
                    }
                }
            }
        }
    }
}