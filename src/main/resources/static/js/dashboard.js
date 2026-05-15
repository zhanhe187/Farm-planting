var statusMap = {
    GROWING: "生长中", READY_HARVEST: "待采收", PLANNED: "已规划",
    SOWED: "已播种", HARVESTING: "采收中", COMPLETED: "已完成", ABANDONED: "已废弃"
};

var plotRects = [];

function drawPlots(hoverIdx) {
    var canvas = document.getElementById("plotCanvas");
    if (!canvas) return;
    var rect = canvas.getBoundingClientRect();
    canvas.width = rect.width * window.devicePixelRatio;
    canvas.height = 330 * window.devicePixelRatio;
    var ctx = canvas.getContext("2d");
    ctx.scale(window.devicePixelRatio, window.devicePixelRatio);
    ctx.clearRect(0, 0, rect.width, 330);
    ctx.fillStyle = "#142018";
    ctx.fillRect(0, 0, rect.width, 330);

    plotRects = [];
    var nodes = document.querySelectorAll(".plot-data");
    for (var i = 0; i < nodes.length; i++) {
        var node = nodes[i];
        var x = Number(node.dataset.x);
        var y = Number(node.dataset.y);
        var w = Number(node.dataset.w);
        var h = Number(node.dataset.h);
        var status = node.dataset.status;
        var color = status === "READY_HARVEST" ? "#d6a044" : status === "GROWING" ? "#4f9a63" : "#6bb9d6";
        ctx.fillStyle = color;
        ctx.strokeStyle = "#0c130f";
        ctx.lineWidth = 2;
        ctx.fillRect(x, y, w, h);
        ctx.strokeRect(x, y, w, h);
        ctx.fillStyle = "#f3f7ec";
        ctx.font = "700 14px Microsoft YaHei";
        ctx.fillText(node.dataset.name, x + 12, y + 24);
        ctx.font = "12px Microsoft YaHei";
        ctx.fillText(statusMap[status] || status, x + 12, y + 46);
        plotRects.push({ x: x, y: y, w: w, h: h, node: node });
    }

    if (hoverIdx != null && hoverIdx >= 0 && hoverIdx < plotRects.length) {
        var p = plotRects[hoverIdx];
        var nd = p.node;
        drawTooltip(ctx, p.x + p.w / 2, p.y - 10, nd);
    }
}

function drawTooltip(ctx, cx, ty, node) {
    var lines = [node.dataset.name];
    if (node.dataset.batch) lines.push("批次: " + node.dataset.batch);
    if (node.dataset.crop) lines.push("作物: " + node.dataset.crop);
    if (node.dataset.sow) lines.push("播种: " + node.dataset.sow);
    if (node.dataset.expect) lines.push("预计采收: " + node.dataset.expect);
    var status = node.dataset.status;
    lines.push("状态: " + (statusMap[status] || status));

    ctx.font = "12px Microsoft YaHei";
    var maxW = 0;
    for (var i = 0; i < lines.length; i++) {
        var lw = ctx.measureText(lines[i]).width;
        if (lw > maxW) maxW = lw;
    }
    var pad = 10;
    var boxW = maxW + pad * 2;
    var boxH = lines.length * 18 + pad * 2;
    var bx = cx - boxW / 2;
    var by = ty - boxH;
    if (by < 0) by = ty + 20;
    if (bx < 0) bx = 4;

    ctx.fillStyle = "rgba(27, 39, 31, 0.95)";
    ctx.strokeStyle = "#8fd06a";
    ctx.lineWidth = 1;
    ctx.beginPath();
    ctx.roundRect(bx, by, boxW, boxH, 6);
    ctx.fill();
    ctx.stroke();

    ctx.fillStyle = "#f3f7ec";
    ctx.font = "12px Microsoft YaHei";
    for (var j = 0; j < lines.length; j++) {
        ctx.fillText(lines[j], bx + pad, by + pad + 14 + j * 18);
    }
}

function getHoverIndex(e) {
    var canvas = document.getElementById("plotCanvas");
    var rect = canvas.getBoundingClientRect();
    var mx = e.clientX - rect.left;
    var my = e.clientY - rect.top;
    for (var i = 0; i < plotRects.length; i++) {
        var p = plotRects[i];
        if (mx >= p.x && mx <= p.x + p.w && my >= p.y && my <= p.y + p.h) return i;
    }
    return -1;
}

function buildChart() {
    if (!window.echarts) return;
    var statusEl = document.getElementById("statusChart");
    if (statusEl) {
        var labels = [];
        var values = [];
        document.querySelectorAll(".status-data").forEach(function(node) {
            labels.push(statusMap[node.dataset.status] || node.dataset.status);
            values.push(Number(node.dataset.value));
        });
        echarts.init(statusEl).setOption({
            color: ["#4f9a63", "#d6a044", "#6bb9d6", "#8aa596", "#ff806f"],
            backgroundColor: "transparent",
            tooltip: { backgroundColor: "#1b271f", borderColor: "#355240", textStyle: { color: "#f3f7ec" } },
            xAxis: { type: "category", data: labels, axisLine: { lineStyle: { color: "#355240" } }, axisLabel: { color: "#a7b6aa" } },
            yAxis: { type: "value", splitLine: { lineStyle: { color: "rgba(143, 208, 106, 0.14)" } }, axisLabel: { color: "#a7b6aa" } },
            series: [{ type: "bar", data: values, barWidth: 28 }]
        });
    }
}

window.addEventListener("load", function() {
    drawPlots(null);
    buildChart();
    var canvas = document.getElementById("plotCanvas");
    if (canvas) {
        var lastIdx = -1;
        canvas.addEventListener("mousemove", function(e) {
            var idx = getHoverIndex(e);
            if (idx !== lastIdx) { lastIdx = idx; drawPlots(idx >= 0 ? idx : null); }
        });
        canvas.addEventListener("mouseleave", function() { lastIdx = -1; drawPlots(null); });
    }
});
window.addEventListener("resize", function() { drawPlots(null); });
