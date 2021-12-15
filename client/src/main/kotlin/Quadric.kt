import vision.gears.webglmath.Mat4
import vision.gears.webglmath.Vec1
import vision.gears.webglmath.Vec3
import vision.gears.webglmath.Vec4
import vision.gears.webglmath.UniformProvider

class Quadric(
  id : Int,
  vararg programs : Program) : UniformProvider("quadrics[$id]") {

	val surface by QuadraticMat4()
	val clipper0 by QuadraticMat4(Mat4(
    0f,0f,0f,0f,
    0f,0f,0f,0f,
    0f,0f,0f,0f,
    0f,0f,0f,0f
  ))
  val invClipper0 by Vec1(0f)
  val clipper1 by QuadraticMat4(Mat4(
    0f,0f,0f,0f,
    0f,0f,0f,0f,
    0f,0f,0f,0f,
    0f,0f,0f,0f
  ))
  val invClipper1 by Vec1(0f)
  val kd by Vec3(1f,1f,1f)
  val kr by Vec3(1f,1f,1f)
  val lace by Vec1(0f)
  init{
    addComponentsAndGatherUniforms(*programs)
  }

  fun scale(sx : Float = 1.0f, sy : Float = 1.0f, sz : Float = 1.0f) {
    surface.scale(sx, sy, sz)
    clipper0.scale(sx, sy, sz)
    clipper1.scale(sx, sy, sz)
  }

  fun rotate(angle : Float = 0.0f, axisX : Float = 0.0f, axisY : Float = 0.0f, axisZ : Float = 0.0f) {
    surface.rotate(angle, axisX, axisY, axisZ)
    clipper0.rotate(angle, axisX, axisY, axisZ)
    clipper1.rotate(angle, axisX, axisY, axisZ)
  }

  fun translate(x : Float = 0.0f, y : Float = 0.0f, z : Float = 0.0f) {
    surface.translate(x, y, z)
    clipper0.translate(x, y, z)
    clipper1.translate(x, y, z)
  }

  companion object {
  }
}
