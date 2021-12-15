import org.w3c.dom.HTMLCanvasElement
import org.khronos.webgl.WebGLRenderingContext as GL //# GL# we need this for the constants declared ˙HUN˙ a constansok miatt kell
import kotlin.js.Date
import vision.gears.webglmath.UniformProvider
import vision.gears.webglmath.Vec1
import vision.gears.webglmath.Vec2
import vision.gears.webglmath.Vec3
import vision.gears.webglmath.Vec4
import vision.gears.webglmath.Mat4
import kotlin.math.exp
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.cos
import kotlin.math.sqrt
import kotlin.math.atan2

class Scene (
  val gl : WebGL2RenderingContext)  : UniformProvider("scene") {

  var nextId = 0
  
  val vsQuad = Shader(gl, GL.VERTEX_SHADER, "quad-vs.glsl")
  val fsTrace = Shader(gl, GL.FRAGMENT_SHADER, "trace-fs.glsl")  
  val traceProgram = Program(gl, vsQuad, fsTrace)
  val quadGeometry = TexturedQuadGeometry(gl)  

  val traceMaterial = Material(traceProgram).apply{
    this["envTexture"]?.set(TextureCube(gl, 
      "media/fall21_posx.png",
      "media/fall21_negx.png",
      "media/fall21_posy.png",
      "media/fall21_negy.png",
      "media/fall21_posz.png",
      "media/fall21_negz.png"))
    this["freq"]?.set(140f)
    this["noiseFreq"]?.set(200f)
    this["noiseExp"]?.set(1f)
    this["noiseAmp"]?.set(140f)
  }

  val traceQuad = Mesh(traceMaterial, quadGeometry)

  val camera = PerspectiveCamera(*Program.all).apply{
    position.set(0f,1f,1f)
    update()
  }

  fun resize(canvas : HTMLCanvasElement) {
    gl.viewport(0, 0, canvas.width, canvas.height)//#viewport# tell the rasterizer which part of the canvas to draw to ˙HUN˙ a raszterizáló ide rajzoljon
    camera.setAspectRatio(canvas.width.toFloat()/canvas.height)
  }

  val timeAtFirstFrame = Date().getTime()
  var timeAtLastFrame =  timeAtFirstFrame

  val quadrics = Array<Quadric>(15) { Quadric(it, *Program.all) }

  val board = Quadric(getId())
  val pawn = ChessPiece()
  val bishop = ChessPiece()
  val king = ChessPiece()
  val queen = ChessPiece()
  val knight = ChessPiece()

  init{
    board.surface.set(
      0.0f, 0.0f, 0.0f, 0.0f, 
      0.0f, 0.0f, 0.0f, 1.0f, 
      0.0f, 0.0f, 0.0f, 0.0f,             
      0.0f, 0.0f, 0.0f, 0.0f
    )
    board.clipper0.set(
      0.0f, 0.0f, 0.0f, 0.0f, 
      0.0f, 0.0f, 0.0f, 0.0f, 
      0.0f, 0.0f, 1.0f, 0.0f,             
      0.0f, 0.0f, 0.0f,-16.0f
    )
    board.clipper1.set(
      1.0f, 0.0f, 0.0f, 0.0f, 
      0.0f, 0.0f, 0.0f, 0.0f, 
      0.0f, 0.0f, 0.0f, 0.0f,             
      0.0f, 0.0f, 0.0f,-16.0f
    )
    // board.kd.set(1f,1f,1f)
    // board.kr.set(0f,0f,0f)
    quadrics[0] = board

    pawn.quadrics.add(Quadric(getId()))
    pawn.get(0).surface.set(
      1.0f, 0.0f, 0.0f, 0.0f, 
      0.0f, -1.0f, 0.0f, 0.0f, 
      0.0f, 0.0f, 1.0f, 0.0f,             
      0.0f, 0.0f, 0.0f, 0.0f
    )
    pawn.get(0).surface.scale(1f,2f,1f)
    pawn.get(0).clipper0.set(
      0.0f, 0.0f, 0.0f, 0.0f, 
      0.0f, 1.0f, 0.0f, 0.0f, 
      0.0f, 0.0f, 0.0f, 0.0f,             
      0.0f, 0.0f, 0.0f, -4.0f
    )
    pawn.get(0).clipper0.translate(0f,-2f)
    pawn.quadrics.add(Quadric(getId()))
    pawn.get(1).surface.set(
      1.0f, 0.0f, 0.0f, 0.0f, 
      0.0f, 1.0f, 0.0f, 0.0f, 
      0.0f, 0.0f, 1.0f, 0.0f,             
      0.0f, 0.0f, 0.0f, -(0.5f*0.5f)
    )
    pawn.scale(.25f,.25f,.25f)
    pawn.translate(.5f,1f,-2.5f)
    pawn.setKD(Vec3(0f,0f,0f))
    pawn.assign(quadrics, 1)


    bishop.quadrics.add(Quadric(getId()))
    bishop.get(0).surface.set(
      1.0f, 0.0f, 0.0f, 0.0f, 
      0.0f, -1.0f, 0.0f, 0.0f, 
      0.0f, 0.0f, 1.0f, 0.0f,             
      0.0f, 0.0f, 0.0f, 0.0f
    )
    bishop.get(0).surface.scale(.5f,2f,.5f)
    bishop.get(0).clipper0.set(
      0.0f, 0.0f, 0.0f, 0.0f, 
      0.0f, 1.0f, 0.0f, 0.0f, 
      0.0f, 0.0f, 0.0f, 0.0f,             
      0.0f, 0.0f, 0.0f, -16.0f
    )
    bishop.get(0).clipper0.translate(0f,-4f)
    bishop.get(0).clipper1.set(
      0.0f, 0.0f, 0.0f, 0.0f,
      0.0f, 1.0f, 0.0f, 0.0f,
      0.0f, 0.0f, 1.0f, 0.0f,            
      0.0f, 0.0f, 0.0f, -(0.2f*0.2f)
    )
    bishop.get(0).invClipper1.set(1f)
    bishop.quadrics.add(Quadric(getId()))
    bishop.get(1).surface.set(
      1.0f, 0.0f, 0.0f, 0.0f, 
      0.0f, 1.0f, 0.0f, 0.0f, 
      0.0f, 0.0f, 1.0f, 0.0f,             
      0.0f, 0.0f, 0.0f, -(0.5f*0.5f)
    )
    bishop.get(1).clipper0.set(
      0.0f, 0.0f, 0.0f, 0.0f,
      0.0f, 1.0f, 0.0f, 0.0f,
      0.0f, 0.0f, 1.0f, 0.0f,            
      0.0f, 0.0f, 0.0f, -(0.2f*0.2f)
    )
    bishop.get(1).invClipper0.set(1f)
    bishop.scale(.25f,.25f,.25f)
    bishop.translate(1.5f,2f,-3.5f)
    bishop.setKD(Vec3(0f,0f,0f))
    bishop.assign(quadrics, 3)


    king.quadrics.add(Quadric(getId()))
    king.get(0).surface.set(
      1.0f, 0.0f, 0.0f, 0.0f, 
      0.0f, -1.0f, 0.0f, 0.0f, 
      0.0f, 0.0f, 1.0f, 0.0f,             
      0.0f, 0.0f, 0.0f, 0.0f
    )
    king.get(0).surface.scale(.5f,3f,.5f)
    king.get(0).clipper0.set(
      0.0f, 0.0f, 0.0f, 0.0f, 
      0.0f, 1.0f, 0.0f, 0.0f, 
      0.0f, 0.0f, 0.0f, 0.0f,             
      0.0f, 0.0f, 0.0f, -36.0f
    )
    king.get(0).clipper0.translate(0f,-6f)
    king.get(0).clipper1.set(
      1.0f, 0.0f, 0.0f, 0.0f, 
      0.0f, 0.0f, 0.0f, -1.0f, 
      0.0f, 0.0f, 1.0f, 0.0f,             
      0.0f, 0.0f, 0.0f, 0.0f
    )
    king.get(0).clipper1.translate(0f, -5f)
    king.get(0).invClipper1.set(1f)
    king.quadrics.add(Quadric(getId()))
    king.get(1).surface.set(
      1.0f, 0.0f, 0.0f, 0.0f, 
      0.0f, 0.0f, 0.0f, -1.0f, 
      0.0f, 0.0f, 1.0f, 0.0f,             
      0.0f, 0.0f, 0.0f, 0.0f
    )
    king.get(1).clipper0.set(
      0.8f, 0.0f, 0.0f, 0.0f,
      0.0f, 0.0f, 0.0f, -1.0f,
      0.0f, 0.0f, 0.0f, 0.0f,            
      0.0f, 0.0f, 0.0f, 1.0f
    )
    king.get(1).invClipper0.set(1f)
    king.get(1).clipper1.set(
      0.0f, 0.0f, 0.0f, 0.0f,
      0.0f, 0.0f, 0.0f, -1.0f,
      0.0f, 0.0f, 0.9f, 0.0f,            
      0.0f, 0.0f, 0.0f, 1.0f
    )
    king.get(1).invClipper1.set(1f)
    king.get(1).translate(0f,-5f)
    king.scale(.25f,.25f,.25f)
    king.translate(-.5f,3f,-3.5f)
    king.setKD(Vec3(0f,0f,0f))
    king.assign(quadrics, 5)


    queen.quadrics.add(Quadric(getId()))
    queen.get(0).surface.set(
      1.0f, 0.0f, 0.0f, 0.0f, 
      0.0f, -1.0f, 0.0f, 0.0f, 
      0.0f, 0.0f, 1.0f, 0.0f,             
      0.0f, 0.0f, 0.0f, 0.0f
    )
    queen.get(0).surface.scale(.3f,3f,.3f)
    queen.get(0).clipper0.set(
      0.0f, 0.0f, 0.0f, 0.0f, 
      0.0f, 1.0f, 0.0f, 0.0f, 
      0.0f, 0.0f, 0.0f, 0.0f,             
      0.0f, 0.0f, 0.0f, -36.0f
    )
    queen.get(0).clipper0.translate(0f,-6f)
    queen.get(0).clipper1.set(
      1.0f, 0.0f, 0.0f, 0.0f, 
      0.0f, 0.0f, 0.0f, -1.0f, 
      0.0f, 0.0f, 1.0f, 0.0f,             
      0.0f, 0.0f, 0.0f, 0.0f
    )
    queen.get(0).clipper1.translate(0f, -5f)
    queen.get(0).invClipper1.set(1f)
    queen.quadrics.add(Quadric(getId()))
    queen.get(1).surface.set(
      1.0f, 0.0f, 0.0f, 0.0f, 
      0.0f, 0.0f, 0.0f, -1.0f, 
      0.0f, 0.0f, 1.0f, 0.0f,             
      0.0f, 0.0f, 0.0f, 0.0f
    )
    queen.get(1).clipper0.set(
      0.8f, 0.0f, 0.0f, 0.0f,
      0.0f, 0.0f, 0.0f, -1.0f,
      0.0f, 0.0f, 0.0f, 0.0f,            
      0.0f, 0.0f, 0.0f, 1.0f
    )
    queen.get(1).invClipper0.set(1f)
    queen.get(1).clipper1.set(
      0.0f, 0.0f, 0.0f, 0.0f,
      0.0f, 0.0f, 0.0f, -1.0f,
      0.0f, 0.0f, 0.9f, 0.0f,            
      0.0f, 0.0f, 0.0f, 1.0f
    )
    queen.get(1).invClipper1.set(1f)
    queen.get(1).translate(0f,-5f)
    queen.quadrics.add(Quadric(getId()))
    queen.get(2).surface.set(
      7.5f, 0.0f, 0.0f, 0.0f, 
      0.0f, -1.0f, 0.0f, 0.0f, 
      0.0f, 0.0f, 7.5f, 0.0f,             
      0.0f, 0.0f, 0.0f, -1.0f
    )
    queen.get(2).lace.set(1f)
    queen.get(2).clipper0.set(
      0.0f, 0.0f, 0.0f, 0.0f, 
      0.0f, 1.0f, 0.0f, 0.0f, 
      0.0f, 0.0f, 0.0f, 0.0f,             
      0.0f, 0.0f, 0.0f, -(2.5f*2.5f)
    )
    queen.get(2).clipper0.translate(0f,-2.5f)
    queen.get(2).translate(0f, -7f)
    queen.scale(.25f,.25f,.25f)
    queen.translate(.5f,3f,-3.5f)
    queen.setKD(Vec3(0f,0f,0f))
    queen.assign(quadrics, 7)

    knight.quadrics.add(Quadric(getId()))
    knight.get(0).surface.set(
      -1.0f, 0.0f, 0.0f, 0.0f, 
      0.0f, 1.0f, 0.0f, 0.0f, 
      0.0f, 0.0f, -1.0f, 0.0f,             
      0.0f, 0.0f, 0.0f, -1.0f
    )
    knight.get(0).clipper0.set(
      0.0f, 0.0f, 0.0f, 0.0f, 
      0.0f, 1.0f, 0.0f, 0.0f, 
      0.0f, 0.0f, 0.0f, 0.0f,             
      0.0f, 0.0f, 0.0f, -1.0f
    )
    knight.get(0).clipper0.translate(0f,-1f)
    knight.get(0).clipper1.set(
      1.0f, 0.0f, 0.0f, 0.0f, 
      0.0f, 1.0f, 0.0f, 0.0f, 
      0.0f, 0.0f, 1.0f, 0.0f,             
      0.0f, 0.0f, 0.0f, -1.0f
    )
    knight.get(0).clipper1.translate(0f,-1.3f,-1.5f)
    knight.get(0).invClipper1.set(1f)
    knight.quadrics.add(Quadric(getId()))
    knight.get(1).surface.set(
      1.0f, 0.0f, 0.0f, 0.0f, 
      0.0f, 0.0f, 0.0f, 0.0f, 
      0.0f, 0.0f, 1.0f, 0.0f,             
      0.0f, 0.0f, 0.0f, -0.4f
    )
    knight.get(1).clipper0.set(
      0.0f, 0.0f, 0.0f, 0.0f, 
      0.0f, 1.0f, 0.0f, 0.0f, 
      0.0f, 0.0f, 0.0f, -0.7f,             
      0.0f, 0.0f, 0.0f, -2.0f
    )
    knight.get(1).clipper0.translate(0f,0f,.1f)
    knight.get(1).clipper1.set(
      0.1f, 0.0f, 0.0f, 0.0f, 
      0.0f, 0.1f, 0.0f, 0.0f, 
      0.0f, 0.0f, 50.0f, 0.0f,             
      0.0f, 0.0f, 0.0f, -1.0f
    )
    knight.get(1).invClipper1.set(1f)
    knight.quadrics.add(Quadric(getId()))
    knight.get(2).surface.set(
      -1.0f, 0.0f, 0.0f, 0.0f, 
      0.0f, 0.0f, 0.0f, -1.0f, 
      0.0f, 0.0f, 1.0f, 0.0f,             
      0.0f, 0.0f, 0.0f, 0.0f
    )
    knight.get(2).clipper0.set(
      0.0f, 0.0f, 0.0f, 0.0f, 
      0.0f, 1.0f, 0.0f, 0.0f, 
      0.0f, 0.0f, 0.0f, 0.0f,             
      0.0f, 0.0f, 0.0f, -1.0f
    )
    knight.get(2).clipper1.set(
      0.0f, 0.0f, 0.0f, 0.0f, 
      0.0f, 0.0f, 0.0f, 0.0f, 
      0.0f, 0.0f, 1.0f, 0.0f,             
      0.0f, 0.0f, 0.0f, -1.0f
    )
    knight.get(2).clipper1.translate(0f,0f,-1f)
    knight.get(2).scale(.5f,.3f, 1f)
    knight.get(2).translate(0f,1.5f,1.5f)
    knight.scale(.25f,.5f,.25f)
    knight.translate(2.5f,1f,-1.5f)
    knight.setKD(Vec3(0f,0f,0f))
    knight.assign(quadrics, 10)

    addComponentsAndGatherUniforms(*Program.all)
  }

  var distTravelled = 0f

  val R = 1f
  val k = 2f
  val lastPosition = Vec2(0f,0f)
  var lastAngle = 0f

  @Suppress("UNUSED_PARAMETER")
  fun update(keysPressed : Set<String>) {
    val timeAtThisFrame = Date().getTime() 
    val dt = (timeAtThisFrame - timeAtLastFrame).toFloat() / 1000.0f
    val t = (timeAtThisFrame - timeAtFirstFrame).toFloat() / 1000.0f
    timeAtLastFrame = timeAtThisFrame

    camera.move(dt, keysPressed)

    if (distTravelled < 6f) {
      pawn.translate(0f,0f,dt)
      distTravelled += dt
    }

    val phi = t%360
    val nextPosition = Vec2(R*cos(k*phi)*cos(phi), R*cos(k*phi)*sin(phi))
    val movement = nextPosition - lastPosition
    lastPosition.set(nextPosition)
    
    knight.translate(movement.x, 0f, movement.y)
    
    
    gl.clearColor(0.3f, 0.0f, 0.3f, 1.0f)//## red, green, blue, alpha in [0, 1]
    gl.clearDepth(1.0f)//## will be useful in 3D ˙HUN˙ 3D-ben lesz hasznos
    gl.clear(GL.COLOR_BUFFER_BIT or GL.DEPTH_BUFFER_BIT)//#or# bitwise OR of flags

    traceQuad.draw(this, camera, *quadrics);
  }

  fun getId() : Int {
    val id = nextId
    nextId++
    return id
  }
}
