const functions = require("firebase-functions/v1");
const express = require("express");
const cors = require("cors");
const OpenAI = require("openai"); // ✅ 新版匯入方式

const app = express();
app.use(cors({ origin: true }));
app.use(express.json());

// ✅ 新版建立 client
const client = new OpenAI({ apiKey: functions.config().openai.key });

// 觸診文案 → 分數映射
const mapTouch = {
  ribs: { "清楚可摸到肋骨": 2, "需按壓才摸到": 5, "摸不到肋骨": 8 },
  waist: { "腰身明顯": 3, "腰身略可見": 5, "無腰身": 8 },
  stomach: { "腹部上提": 2, "腹部平坦": 5, "腹部下垂": 8 },
};

app.post("/bcs-score", async (req, res) => {
  try {
    const { imageUrl, ribs, waist, stomach } = req.body;
    if (!imageUrl || !ribs || !waist || !stomach) {
      return res.status(400).json({ error: "Missing fields" });
    }

    // ✅ 新版 Chat Completions 呼叫
    const completion = await client.chat.completions.create({
      model: "gpt-4o-mini", // 換成你帳號可用的支援看圖模型
      response_format: {
        type: "json_schema",
        json_schema: {
          name: "bcs_result",
          schema: {
            type: "object",
            required: ["image_score", "final_bcs", "confidence", "notes"],
            properties: {
              image_score: { type: "integer", minimum: 1, maximum: 9 },
              final_bcs: { type: "integer", minimum: 1, maximum: 9 },
              confidence: { type: "number", minimum: 0, maximum: 1 },
              notes: { type: "string" }
            }
          }
        }
      },
      messages: [
        {
          role: "system",
          content: "你是寵物營養助理。回傳 JSON，BCS為1~9。"
        },
        {
          role: "user",
          content: [
            {
              type: "text",
              text:
`這是狗狗的照片與觸診描述：
- 肋骨：${ribs}
- 腰部：${waist}
- 腹部：${stomach}

請：
1) 估照片的 image_score(1-9)。
2) 綜合觸診給 final_bcs(1-9)；一致時允許 1 或 9。
3) 僅回 JSON（image_score, final_bcs, confidence, notes）。`
            },
            { type: "image_url", image_url: { url: imageUrl } }
          ]
        }
      ],
      temperature: 0.2
    });

    const ai = JSON.parse(completion.choices[0].message.content || "{}");

    // 後端保險校正
    const ribsScore = mapTouch.ribs[ribs] ?? 5;
    const waistScore = mapTouch.waist[waist] ?? 5;
    const stomachScore = mapTouch.stomach[stomach] ?? 5;
    const touchAvg = (ribsScore + waistScore + stomachScore) / 3;

    let image_score = Number.isInteger(ai.image_score) ? ai.image_score : 5;
    let final_bcs   = Number.isInteger(ai.final_bcs)   ? ai.final_bcs   : 5;

    if (image_score <= 2 && touchAvg <= 3) final_bcs = 1;
    else if (image_score >= 8 && touchAvg >= 7) final_bcs = 9;
    else final_bcs = Math.round((image_score * 2 + touchAvg) / 3);

    if (Math.abs(image_score - touchAvg) >= 3) {
      final_bcs = Math.round((final_bcs + (image_score + touchAvg) / 2) / 2);
    }
    final_bcs = Math.min(9, Math.max(1, final_bcs));

    return res.json({
      image_score,
      final_bcs,
      confidence: typeof ai.confidence === "number" ? ai.confidence : 0.6,
      notes: typeof ai.notes === "string" ? ai.notes : "後端已做一次校正。",
      touch_scores: { ribs: ribsScore, waist: waistScore, stomach: stomachScore }
    });
  } catch (err) {
    console.error(err);
    return res.status(500).json({ error: "OpenAI call failed" });
  }
});

// 設定地區（可用 asia-east1），並匯出
exports.api = functions.region("asia-east1").https.onRequest(app);
