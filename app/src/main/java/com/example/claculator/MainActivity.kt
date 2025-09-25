package com.example.claculator  // <-- change this if your project package is different

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CalculatorApp()
        }
    }
}

@Composable
fun CalculatorApp() {
    var input by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(16.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Display
        Column(modifier = Modifier
            .fillMaxWidth()
            .offset(x = 0.dp, y = 350.dp)) {
            Text(
                text = if (input.isEmpty()) "0" else input,
                color = Color.White,
                fontSize = 40.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                maxLines = 1
            )
        }

        // Buttons
        val rows = listOf(
            listOf("C", "⌫", "%", "/"),
            listOf("7", "8", "9", "*"),
            listOf("4", "5", "6", "-"),
            listOf("1", "2", "3", "+"),
            listOf("0", ".", "=")
        )

        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            for (row in rows) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    for (label in row) {
                        val weight = if (label == "0") 2f else 1f
                        CalculatorButton(
                            text = label,
                            modifier = Modifier
                                .weight(weight)
                                .height(72.dp)
                        ) {
                            when (label) {
                                "C" -> input = ""
                                "⌫" -> if (input.isNotEmpty()) input = input.dropLast(1)
                                "=" -> input = evaluateExpression(input)
                                else -> input += label
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CalculatorButton(text: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (text in listOf(
                    "/",
                    "*",
                    "-",
                    "+",
                    "="
                )
            ) Color(0xFFEF6C00) else Color.DarkGray,
            contentColor = Color.White
        )
    ) {
        Text(text = text, fontSize = 25.sp)
    }
}

/** SAFE expression evaluator implemented in Kotlin:
 * supports + - * / % and decimal numbers and parentheses.
 */
fun evaluateExpression(input: String): String {
    if (input.isBlank()) return ""
    var expr = input.replace('×', '*').replace('÷', '/').replace('x', '*')
    // handle leading unary minus:
    if (expr.startsWith("-")) expr = "0$expr"
    expr = expr.replace("(-", "(0-")

    return try {
        val tokens = tokenize(expr)
        val rpn = shuntingYard(tokens)
        val value = evalRPN(rpn)
        if (value.isNaN() || value.isInfinite()) "Error"
        else {
            // Format: if integer, show without .0
            val longVal = value.toLong()
            if (value == longVal.toDouble()) longVal.toString() else value.toString()
        }
    } catch (e: Exception) {
        "Error"
    }
}

private fun tokenize(s: String): List<String> {
    val tokens = mutableListOf<String>()
    var i = 0
    while (i < s.length) {
        val c = s[i]
        when {
            c.isWhitespace() -> i++
            c.isDigit() || c == '.' -> {
                val sb = StringBuilder()
                while (i < s.length && (s[i].isDigit() || s[i] == '.')) {
                    sb.append(s[i])
                    i++
                }
                tokens.add(sb.toString())
            }

            c == '+' || c == '-' || c == '*' || c == '/' || c == '%' || c == '(' || c == ')' -> {
                tokens.add(c.toString())
                i++
            }

            else -> throw IllegalArgumentException("Invalid char: $c")
        }
    }
    return tokens
}

private fun precedence(op: String): Int = when (op) {
    "+", "-" -> 1
    "*", "/", "%" -> 2
    else -> 0
}

private fun shuntingYard(tokens: List<String>): List<String> {
    val output = mutableListOf<String>()
    val ops = ArrayDeque<String>()
    for (t in tokens) {
        if (t.toDoubleOrNull() != null) {
            output.add(t)
        } else if (t == "(") {
            ops.addFirst(t)
        } else if (t == ")") {
            while (ops.isNotEmpty() && ops.first() != "(") {
                output.add(ops.removeFirst())
            }
            if (ops.isNotEmpty() && ops.first() == "(") ops.removeFirst()
        } else { // operator
            while (ops.isNotEmpty() && precedence(ops.first()) >= precedence(t)) {
                output.add(ops.removeFirst())
            }
            ops.addFirst(t)
        }
    }
    while (ops.isNotEmpty()) output.add(ops.removeFirst())
    return output
}

private fun evalRPN(rpn: List<String>): Double {
    val st = ArrayDeque<Double>()
    for (t in rpn) {
        val num = t.toDoubleOrNull()
        if (num != null) {
            st.addFirst(num)
        } else {
            val b = st.removeFirstOrNull() ?: throw IllegalArgumentException("Invalid")
            val a = st.removeFirstOrNull() ?: throw IllegalArgumentException("Invalid")
            val res = when (t) {
                "+" -> a + b
                "-" -> a - b
                "*" -> a * b
                "/" -> a / b
                "%" -> a % b
                else -> throw IllegalArgumentException("Unknown op $t")
            }
            st.addFirst(res)
        }
    }
    return st.firstOrNull() ?: Double.NaN
}

@Preview(showSystemUi = true)
@Composable
fun PreviewCalculator() {
    CalculatorApp()
}
