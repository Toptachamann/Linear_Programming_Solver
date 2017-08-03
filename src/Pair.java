

public class Pair<L, R> {
	private L left;
	private R right;

	public Pair() {

	}

	public Pair(L left, R right) {
		this.left = left;
		this.right = right;
	}

	public L first() {
		return left;
	}

	public R second() {
		return right;
	}

	public void setFirst(L l) {
		left = l;
	}

	public void setSecond(R r) {
		right = r;
	}

	public String toString() {
		return left.toString() + " " + right.toString();
	}

}
