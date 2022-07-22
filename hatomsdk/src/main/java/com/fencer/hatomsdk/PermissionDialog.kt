package com.fencer.hatomsdk

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import com.blankj.utilcode.util.ScreenUtils
import com.permissionx.guolindev.dialog.RationaleDialog

class DialogPermissionBinding{

       lateinit var root:View

    lateinit var  contentText : TextView
    lateinit var  rightButton: TextView
    lateinit var  leftButton :TextView

    companion object{

        fun inflate(inflater: LayoutInflater) :DialogPermissionBinding{
            val binding = DialogPermissionBinding()
            binding.root = inflater.inflate(R.layout.dialog_permission,null)
            binding.contentText = binding.root.findViewById(R.id.contentText)
            binding.rightButton =binding.root.findViewById(R.id.rightButton)
            binding.leftButton = binding.root.findViewById(R.id.leftButton)
            return  binding
        }
    }
}


class PermissionDialog(
    context: Context,
    private val message: String,
    private val rightText: String,
    private val permissions: MutableList<String>
) : RationaleDialog(context, R.style.PermissionDialog) {

    private lateinit var viewBinding: DialogPermissionBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBinding = DialogPermissionBinding.inflate(LayoutInflater.from(context))
        setContentView(viewBinding.root)
        viewBinding.contentText.text = message
        viewBinding.rightButton.text = rightText
        window?.let {
            val param = it.attributes
            val width = if (ScreenUtils.isPortrait()) {
                (context.resources.displayMetrics.widthPixels * 0.8).toInt()
            } else {
                (context.resources.displayMetrics.heightPixels * 0.8).toInt()
            }
            val height = param.height
            it.setLayout(width, height)
        }
    }


    override fun getNegativeButton(): View {
        return viewBinding.leftButton
    }

    override fun getPositiveButton(): View {
        return viewBinding.rightButton
    }

    override fun getPermissionsToRequest(): MutableList<String> {
        return permissions
    }

}