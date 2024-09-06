package ba.sake.squery

// - a marker typeclass for non-scalar types
// i.e. array-type and similar

// - this is to prevent infinite recursion of Array[Array[Array...T
// the SqlWrite, SqlRead typeclasses would break

trait SqlNonScalarType[T]

given [T]: SqlNonScalarType[Array[T]] = new {}
given [T]: SqlNonScalarType[Seq[T]] = new {}
given [T]: SqlNonScalarType[List[T]] = new {}
given [T]: SqlNonScalarType[Vector[T]] = new {}
