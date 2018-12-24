package com.jiangkang.ktools.effect.fragment

import android.animation.*
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Path
import android.graphics.drawable.AnimatedVectorDrawable
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.media.session.PlaybackStateCompat
import android.view.*
import android.view.animation.BounceInterpolator
import android.view.animation.PathInterpolator
import android.widget.*
import com.jiangkang.container.fragment.ViewDataBinder
import com.jiangkang.container.loadFragment
import com.jiangkang.ktools.R
import com.jiangkang.ktools.animation.CardFlipActivity
import com.jiangkang.ktools.animation.SpringAnimationActivity
import com.jiangkang.tools.device.screenHeight
import com.jiangkang.tools.device.screenSize
import com.jiangkang.tools.device.screenWidth
import com.jiangkang.tools.utils.ToastUtils
import com.jiangkang.tools.widget.KDialog
import com.jiangkang.widget.view.TaiChiView
import kotlinx.android.synthetic.main.fragment_effect.*
import org.jetbrains.anko.backgroundColor


val colors = arrayOf(
        Color.RED,
        Color.BLUE,
        Color.GREEN,
        Color.CYAN,
        Color.DKGRAY,
        Color.GRAY,
        Color.LTGRAY,
        Color.MAGENTA,
        Color.YELLOW
)

/**
 * 动效相关Demo
 */
class EffectFragment : Fragment() {

    private lateinit var mContext: Context

    override fun onAttach(context: Context?) {
        super.onAttach(context)
        mContext = context!!
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_effect, container, false)
        return view
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        handleViewClicked()
        handleCrossFadeView()
        handleFlipCard()
        handleSpringAnimation()
        handleCircleRevealAnimation()
        handleCurvedMotion()
    }

    /**
     * 通过PathInterpolator实现曲线运动
     */
    private fun handleCurvedMotion() {
        btnCurvedMotionAnim.setOnClickListener {
            activity?.loadFragment(
                    R.layout.fragment_value_animator,
                    "Curved Motion Animation",
                    object : ViewDataBinder {
                        override fun bindView(view: View) {
                            val ivDog = view.findViewById<ImageView>(R.id.iv_dog)

                            val path = Path().apply {
                                arcTo(0f, 0f, 1000f, 1000f, 270f, -180f, true)
                            }
                            val animator = ObjectAnimator.ofFloat(ivDog, View.X, View.Y, path).apply {
                                duration = 5000
                                repeatMode = ObjectAnimator.REVERSE
                                start()
                            }

                        }

                    }
            )
        }
    }


    /**
     * 圆形显示动画
     */
    private fun handleCircleRevealAnimation() {
        btnCircleRevealAnim.setOnClickListener {
            activity?.loadFragment(
                    R.layout.fragment_value_animator,
                    "Circle Reveal Animation",
                    object : ViewDataBinder {
                        override fun bindView(view: View) {
                            val ivDog = view.findViewById<ImageView>(R.id.iv_dog)

                            val cx = ivDog.width / 2
                            val cy = ivDog.height / 2
                            val finalRadius = Math.hypot(cx.toDouble(), cy.toDouble()).toFloat()
                            val anim = ViewAnimationUtils.createCircularReveal(ivDog, cx, cy, 0f, finalRadius)
                            ivDog.visibility = View.VISIBLE
                            anim.duration = 5000
                            ivDog.postDelayed({ anim.start() }, 3000)

                            ivDog.setOnClickListener {
                                val cx = ivDog.width / 2
                                val cy = ivDog.height / 2
                                val finalRadius = Math.hypot(cx.toDouble(), cy.toDouble()).toFloat()
                                val anim = ViewAnimationUtils.createCircularReveal(ivDog, cx, cy, finalRadius, 0f)
                                anim.duration = 5000
                                anim.addListener(object : AnimatorListenerAdapter() {
                                    override fun onAnimationEnd(animation: Animator?) {
                                        ivDog.visibility = View.INVISIBLE
                                        ToastUtils.showShortToast("小狗不见了！")
                                    }
                                })
                                anim.start()
                            }
                        }

                    }
            )
        }
    }

    /**
     * Spring Animation
     */
    private fun handleSpringAnimation() {
        btnSpringAnim.setOnClickListener {
            activity?.startActivity(Intent(activity, SpringAnimationActivity::class.java))
        }
    }

    private fun handleFlipCard() {
        btnCardFlip.setOnClickListener {
            activity?.startActivity(Intent(activity, CardFlipActivity::class.java))
        }
    }

    //实际上就是淡入淡出效果，调整透明度动画
    private fun handleCrossFadeView() {
        btnCrossFadeAnimator.setOnClickListener {
            activity?.loadFragment(
                    R.layout.layout_crossfade,
                    "CrossFade Animator",
                    object : ViewDataBinder {
                        override fun bindView(view: View) {
                            val contentView = view.findViewById<ScrollView>(R.id.content)
                            val loadingView = view.findViewById<ProgressBar>(R.id.loading_spinner)
                            var animDuration = 2000
                            contentView.visibility = View.GONE

                            contentView.apply {
                                alpha = 0f
                                visibility = View.VISIBLE
                                animate()
                                        .alpha(1.0f)
                                        .setDuration(animDuration.toLong())
                                        .setListener(null)
                            }

                            loadingView.animate()
                                    .alpha(0f)
                                    .setDuration(animDuration.toLong())
                                    .setListener(object : AnimatorListenerAdapter() {
                                        override fun onAnimationEnd(animation: Animator) {
                                            loadingView.visibility = View.GONE
                                        }
                                    })
                        }
                    }
            )
        }

    }

    private fun handleViewClicked() {

        effect_shape.setOnClickListener {
            handleClick(ShapeViewFragment())
        }

        tai_chi.setOnClickListener {
            val taiChiView = TaiChiView(this@EffectFragment.activity)
            KDialog.showCustomViewDialog(this@EffectFragment.activity, "太极图", taiChiView, { dialog, which -> dialog.dismiss() }) { dialog, which -> dialog.dismiss() }
            taiChiView.startRotate()
        }

        animated_shape_view.setOnClickListener {
            handleClick(AnimatedShapeViewFragment())
        }

        shape_path_view.setOnClickListener {
            handleClick(ShapePathViewFragment())
            val (x, y) = mContext.screenSize
        }

        //自动布局动画
        btnAutoLayoutAnimation.setOnClickListener {
            activity?.loadFragment(
                    R.layout.fragment_auto_animation_layout,
                    "Auto Animation Layout Updates",
                    object : ViewDataBinder {
                        @SuppressLint("ObjectAnimatorBinding")
                        override fun bindView(view: View) {
                            if (view is ViewGroup) {
                                view.layoutTransition = LayoutTransition().apply {
                                    val screenWidth = mContext.screenWidth.toFloat()
                                    setAnimator(
                                            LayoutTransition.APPEARING,
                                            ObjectAnimator.ofFloat(null, "translationX", screenWidth, 0f))
                                    setDuration(LayoutTransition.APPEARING, 1000)

                                    setAnimator(
                                            LayoutTransition.DISAPPEARING,
                                            ObjectAnimator.ofFloat(null, "translationX", 0f, -screenWidth))
                                    setDuration(LayoutTransition.DISAPPEARING, 1000)

                                }
                                val btnAdd = view.findViewById<Button>(R.id.btn_add_view)
                                val btnRemove = view.findViewById<Button>(R.id.btn_remove_view)
                                val layoutParams = LinearLayout.LayoutParams(
                                        LinearLayout.LayoutParams.WRAP_CONTENT,
                                        LinearLayout.LayoutParams.WRAP_CONTENT
                                ).apply {
                                    gravity = Gravity.CENTER_HORIZONTAL
                                    topMargin = 6
                                }
                                btnAdd.setOnClickListener {
                                    view.addView(Button(view.context).apply {
                                        text = view.childCount.toString()
                                        gravity = Gravity.CENTER
                                        backgroundColor = colors[IntRange(0, colors.size - 1).random()]
                                        setOnClickListener { ToastUtils.showShortToast(this@apply.text.toString()) }
                                    }, layoutParams)
                                }
                                btnRemove.setOnClickListener {
                                    if (view.childCount > 1) {
                                        view.removeViewAt(view.childCount - 1)
                                    } else {
                                        ToastUtils.showShortToast("没有View可以移除咯！")
                                    }
                                }
                            }
                        }

                    }
            )
        }

        //ValueAnimator的动效
        btnValueAnimator.setOnClickListener {
            activity?.loadFragment(
                    R.layout.fragment_value_animator,
                    "Value Animator",
                    object : ViewDataBinder {
                        override fun bindView(view: View) {
                            val ivDog = view.findViewById<ImageView>(R.id.iv_dog)
                            val translationXAnimator = ValueAnimator.ofFloat(0f, mContext.screenWidth.toFloat())
                            val translationYAnimator = ValueAnimator.ofFloat(0f, mContext.screenHeight.toFloat())
                            translationXAnimator.addUpdateListener { value ->
                                ivDog.translationX = value.animatedValue as Float
                            }
                            translationYAnimator.addUpdateListener { value ->
                                ivDog.translationY = value.animatedValue as Float
                            }
                            val set = AnimatorSet()
                            set.duration = 10000
                            set.play(translationXAnimator).with(translationYAnimator)
                            set.interpolator = BounceInterpolator()
                            set.start()
                        }
                    }
            )
        }

        btnVectorDrawable.setOnClickListener {
            activity?.loadFragment(
                    R.layout.fragment_vector_drawable,
                    "Vector Drawable",
                    object : ViewDataBinder {
                        override fun bindView(view: View) {
                            val drawable = view.findViewById<ImageView>(R.id.iv_animated_vector_drawable).drawable as AnimatedVectorDrawable
                            drawable.start()
                        }
                    })
        }

    }

    private fun handleClick(fragment: Fragment) {
        val tag = fragment.javaClass.toString()
        this.activity?.apply {
            supportFragmentManager
                    .beginTransaction()
                    .addToBackStack(tag)
                    .replace(android.R.id.content, fragment, tag)
                    .commit()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
    }

}
