# LuaBridge

A method binding extension library for [LuaJ](http://www.luaj.org/luaj/3.0/README.html).

## Usage

First, define your Java class:
```Java
// Java
public class Point extends ExposedJavaClass {
    private int mX, mY;

    public Point(int x, int y) {
        mX = x;
        mY = y;
    }

    @ExposeToLua
    public Point setX(int x) {
        mX = x;
        return this;
    }

    @ExposeToLua
    public Point setY(int y) {
        mY = y;
        return this;
    }

    @ExposeToLua
    public int getX() {
        return mX;
    }

    @ExposeToLua
    public int getY() {
        return mY;
    }

    @Override
    public String toString() {
        return "(" + mX + "," + mY + ")";
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof Point)) {
            return false;
        }
        Point otherPoint = (Point)other;
        return mX == otherPoint.mX && mY == otherPoint.mY;
    }
}
```

Then, create an instance and expose it to the Lua environment:
```Java
// Java
Globals globals = ...;
globals.set("pointA", new Point(3, 4));
globals.set("pointB", new Point(4, 5));
```

Now, call your method just like it's Lua code!
```Lua
-- Lua
pointA:setX(4):setY(5)
assert(pointA:getX() == 4)
assert(pointA:getY() == 5)
assert(tostring(pointA) == "(4,5)")
assert(pointA == pointB)
```

## License

Distributed under the [MIT License](http://opensource.org/licenses/MIT).
