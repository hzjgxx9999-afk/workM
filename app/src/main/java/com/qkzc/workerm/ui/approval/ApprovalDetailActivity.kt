package com.qkzc.workerm.ui.approval

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import com.qkzc.workerm.databinding.ActivityApprovalDetailBinding

class ApprovalDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityApprovalDetailBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityApprovalDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(binding.approvalDetailRoot) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.updatePadding(left = bars.left, top = bars.top, right = bars.right, bottom = bars.bottom)
            insets
        }
        binding.backButton.setOnClickListener { finish() }
        render(intent.getStringExtra(EXTRA_TYPE).orEmpty())
    }

    private fun render(type: String) {
        binding.attachmentCard.isVisible = type == TYPE_REISSUE
        binding.tipText.isVisible = type == TYPE_LOAN
        when (type) {
            TYPE_LEAVE -> {
                binding.titleText.text = "请假申请详情"
                binding.applicantIcon.text = "假"
                binding.applicantName.text = "李强    木工"
                binding.applicantPhone.text = "手机号：139****2468"
                binding.applicantNo.text = "工号：MG20240519032"
                binding.mainInfoTitle.text = "请假信息"
                binding.mainInfoText.text = "请假类型                                      事假\n开始时间              2024-05-22（周三）08:00\n结束时间              2024-05-22（周三）18:00\n请假时长                                      1天\n请假天数                                      1天"
                binding.reasonTitle.text = "请假原因"
                binding.reasonText.text = "家中孩子生病，需要带孩子去医院就诊。"
                binding.teamText.text = "木工班    组长：陈师傅"
            }
            TYPE_LOAN -> {
                binding.titleText.text = "借支申请详情"
                binding.applicantIcon.text = "借"
                binding.applicantName.text = "王建国    架子工"
                binding.applicantPhone.text = "手机号：137****9988"
                binding.applicantNo.text = "工号：QZ20240518027"
                binding.mainInfoTitle.text = "借支信息"
                binding.mainInfoText.text = "本月出勤天数                              18天\n申请借支金额                        ¥1,500.00\n大写金额                              壹仟伍佰元整\n借支用途                                  生活费用\n计划还款时间           2024-06-15（预发薪日）\n还款方式                            从下次工资中抵扣"
                binding.reasonTitle.text = "历史借支记录"
                binding.reasonText.text = "2024-04-15          ¥2,000.00          已还款\n2024-03-10          ¥1,000.00          已还款"
                binding.teamText.text = "架子班    组长：孙师傅"
            }
            else -> {
                binding.titleText.text = "补卡申请详情"
                binding.applicantIcon.text = "人"
                binding.applicantName.text = "张伟    钢筋工"
                binding.applicantPhone.text = "手机号：138****5678"
                binding.applicantNo.text = "工号：GJ20240520045"
                binding.mainInfoTitle.text = "补卡信息"
                binding.mainInfoText.text = "补卡类型                                      下班补卡\n缺卡日期                          2024-05-19（周日）\n缺卡时间                                      18:05（下班）"
                binding.reasonTitle.text = "申请原因"
                binding.reasonText.text = "当天下午在 18:00 之后加班绑扎钢筋，忘记打卡，请批准补卡。"
                binding.teamText.text = "钢筋班    组长：刘师傅"
            }
        }
    }

    companion object {
        const val EXTRA_TYPE = "approval_type"
        const val TYPE_REISSUE = "reissue"
        const val TYPE_LEAVE = "leave"
        const val TYPE_LOAN = "loan"
    }
}
