import vision.gears.webglmath.Vec3

class ChessPiece() {
    val quadrics = ArrayList<Quadric>()

    fun scale(sx : Float = 1.0f, sy : Float = 1.0f, sz : Float = 1.0f) {
        quadrics.forEach {
            it.scale(sx, sy, sz)
        }
    }

    fun rotate(angle : Float = 0.0f, axisX : Float = 0.0f, axisY : Float = 0.0f, axisZ : Float = 0.0f) {
        quadrics.forEach {
            it.rotate(angle, axisX, axisY, axisZ)
        }
    }

    fun translate(x : Float = 0.0f, y : Float = 0.0f, z : Float = 0.0f) {
        quadrics.forEach {
            it.translate(x, y, z)
        }
    }

    fun setKD(color: Vec3) {
        quadrics.forEach {
            it.kd.set(color)
        }
    }

    fun assign(quads: Array<Quadric>, index: Int) {
        var i = index
        quadrics.forEach {
            quads[i] = it
            i++
        }
    }

    fun get(i: Int) : Quadric {
        return quadrics.get(i)
    }
}