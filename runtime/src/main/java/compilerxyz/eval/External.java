package compilerxyz.eval;

@SuppressWarnings("rawtypes")
public record External(Class hostClass, Object hostValue) {

  public Object externalHostValue() {
    return hostValue;
  }

  public Class externalHostClass() {
    return hostClass;
  }
}
