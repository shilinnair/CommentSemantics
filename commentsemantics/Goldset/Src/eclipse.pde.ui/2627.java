/**
 * Test unsupported @nooverride tag on methods in an interface in the default package
 */
public interface test8 {

    /**
	 * @nooverride
	 * @return
	 */
    public int m1();

    /**
	 * @nooverride
	 * @return
	 */
    public abstract char m2();
}
