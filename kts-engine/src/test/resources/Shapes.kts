import org.ostelco.prime.kts.engine.Shape

class Circle : Shape {

    private var radius: Double? = null

    override fun setParam(name: String, value: Double) {
        if (name.toLowerCase() == "radius") {
            radius = value
        }
    }

    override fun getArea(): Double = radius?.let { Math.PI * it * it } ?: throw Exception("Circle's radius not set")
}

class Square : Shape {

    private var side: Double? = null

    override fun setParam(name: String, value: Double) {
        if (name.toLowerCase() == "side") {
            side = value
        }
    }

    override fun getArea(): Double = side?.let { it * it } ?: throw Exception("Square's side not set")
}

class Rectangle : Shape {

    private var height: Double? = null
    private var width: Double? = null

    override fun setParam(name: String, value: Double) {
        when(name.toLowerCase()) {
            "height" -> height = value
            "width" -> width = value
        }
    }

    override fun getArea(): Double = height?.times(
            width ?: throw Exception("Rectangle's width not set"))
            ?: throw Exception("Rectangle's height not set")
}

Circle()