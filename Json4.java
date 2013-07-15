import java.io.Reader;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Json2 {
	private CharSequence cs;
	private int off;

	private static DateFormat df = new SimpleDateFormat("EEE dd MMM hh:mm:ss ZZZ yyyy"); 

	private static HashMap<String, String> monthmap = new HashMap<String, String>();
	static {
		monthmap.put("Jan", "01");
		monthmap.put("Feb", "02");
		monthmap.put("Mar", "03");
		monthmap.put("Apr", "04");
		monthmap.put("May", "05");
		monthmap.put("Jun", "06");
		monthmap.put("Jul", "07");
		monthmap.put("Aug", "08");
		monthmap.put("Sep", "09");
		monthmap.put("Oct", "10");
		monthmap.put("Nov", "11");
		monthmap.put("Dec", "12");
	}

	private static void handle(Object o, String name) {
		if (o instanceof List) {
			List l = (List) o;

			for (int i = 0; i < l.size(); i++) {
				handle(l.get(i), name);
			}
		} else {
			Map<String, Object> map = (Map<String, Object>) o;
			Map<String, Object> geo = (Map<String, Object>) map.get("geo");
			Map<String, Object> user = (Map<String, Object>) map.get("user");

			if (geo != null) {
				List<Double> coordinates = (List<Double>) geo.get("coordinates");

				String text = (String) map.get("text");
				String screen_name = (String) user.get("screen_name");
				String created_at = (String) map.get("created_at");

				if (screen_name == null) {
					screen_name = name;
				}

	// Sun May 15 02:55:14 +0000 2011
				String mon = created_at.substring(4, 7);
				String day = created_at.substring(8, 10);
				String time = created_at.substring(11, 19);
				String year = created_at.substring(26, 30);

				String printdate = year + "-" + monthmap.get(mon) + "-" + day + " " + time;

				System.out.println(screen_name + " " + printdate + " " +
					coordinates.get(0) + "," + coordinates.get(1) + " " + quote(text));
			}
		}
	}

	private static StringBuilder quote(String text) {
		StringBuilder sb = new StringBuilder();

		int len = text.length();
		for (int i = 0; i < len; i++) {
			char c = text.charAt(i);

			if (c == '\\') {
				sb.append("\\\\");
			} else if (c < ' ' || c > '~') {
				if (c >= 0x1000) {
					sb.append("\\u" + Integer.toHexString(c));
				} else if (c >= 0x100) {
					sb.append("\\u0" + Integer.toHexString(c));
				} else if (c >= 0x10) {
					sb.append("\\u00" + Integer.toHexString(c));
				} else {
					sb.append("\\u000" + Integer.toHexString(c));
				}
			} else {
				sb.append(c);
			}
		}

		return sb;
	}

	private void skip() {
		while (off < cs.length()) {
			char c = cs.charAt(off);

			if (c == ' ' || c == '\t' || c == '\r' || c == '\n') {
				off++;
				continue;
			} else {
				break;
			}
		}
	}

	public Object read() {
		char c;

		skip();
		c = cs.charAt(off++);

		if (c == '\"') {
			return readString();
		} else if (c == '{') {
			return readObject();
		} else if (c == '[') {
			return readArray();
		} else if (c == '-' || (c >= '0' && c <= '9')) {
			off--;
			return readNumber();
		} else {
			off--;
			return readLiteral();
		}
	}

	private String readString() {
		char c;
		StringBuilder sb = new StringBuilder();

		while (true) {
			c = cs.charAt(off++);

			if (c == '\"') {
				return sb.toString();
			} else if (c == '\\') {
				c = cs.charAt(off++);

				switch (c) {
				case 'b':
					sb.append('\b');
					break;

				case 'f':
					sb.append('\f');
					break;

				case 'n':
					sb.append('\n');
					break;

				case 'r':
					sb.append('\r');
					break;

				case 't':
					sb.append('\t');
					break;

				case 'u':
					String s = cs.subSequence(off, off + 4).toString();
					off += 4;
					sb.append((char) Integer.parseInt(s, 16));
					break;

				default:
					sb.append(c);
				}
			} else  {
				sb.append(c);
			}
		}
	}

	private List<Object> readArray() {
		ArrayList<Object> al = new ArrayList<Object>();

		while (off < cs.length()) {
			skip();

			if (off < cs.length() && cs.charAt(off) == ']') {
				off++;
				return al;
			}

			al.add(read());
			skip();

			if (off < cs.length() && cs.charAt(off) == ',') {
				off++;
			}
		}

		return al;
	}

	private Map<String, Object> readObject() {
		Map<String, Object> m = new HashMap<String, Object>();

		while (off < cs.length()) {
			skip();

			if (off < cs.length() && cs.charAt(off) == '}') {
				off++;
				return m;
			}

			skip();

			String key = (String) read();

			if (off < cs.length() && cs.charAt(off) == ':') {
				off++;
			}

			Object value = read();

			m.put(key, value);

			skip();

			if (off < cs.length() && cs.charAt(off) == ',') {
				off++;
			}
		}

		return m;
	}

	private Double readNumber() {
		int start = off;

		while (off < cs.length()) {
			char c = cs.charAt(off);

			if (c != '-' && c != '.' && c != 'e' && c != 'E' && c != '+' &&
				(c < '0' || c > '9')) {
				break;
			}

			off++;
		}

		return Double.parseDouble(cs.subSequence(start, off).toString());
	}

	private Object readLiteral() {
		if (off + 4 <= cs.length()) {
			if (cs.subSequence(off, off + 4).toString().equals("null")) {
				off += 4;
				return null;
			}

			if (cs.subSequence(off, off + 4).toString().equals("true")) {
				off += 4;
				return Boolean.valueOf(true);
			}
		}

		if (off + 5 <= cs.length()) {
			if (cs.subSequence(off, off + 5).toString().equals("false")) {
				off += 5;
				return Boolean.valueOf(false);
			}
		}

		throw new RuntimeException("bad literal " + cs.subSequence(off, cs.length()));
	}

	public static void main(String[] argv) {
		try {
			byte[] buf = new byte[5];
			int used = 0;
			InputStream fi;

			if (argv.length == 0) {
				fi = System.in;
			} else {
				fi = new FileInputStream(argv[0]);
			}

			String name = argv[0];
			int ix = name.indexOf('.');
			if (ix != 0) {
				name = name.substring(0, ix);
			}

			while (true) {
				if (false) {
					for (int i = 0; i < used; i++) {
						if (buf[i] == '\n') {
							i++;

							try {
								Json2 j = new Json2();
								String s = new String(buf, 0, i);
								j.cs = s;

								handle(j.read(), name);
							} catch (Throwable t) {
								t.printStackTrace();
							}

							System.arraycopy(buf, i, buf, 0, used - i);
							used -= i;

							break;
						}
					}
				}

				if (used == buf.length) {
					byte[] nb = new byte[buf.length + 1000];
					System.arraycopy(buf, 0, nb, 0, used);
					buf = nb;
				}

				int n = fi.read(buf, used, buf.length - used);

				if (n < 0) {
					break;
				} else {
					used += n;
				}
			}

			if (used > 0) {
				Json2 j = new Json2();
				String s = new String(buf, 0, used);
				j.cs = s;

				handle(j.read(), argv[0]);
			}
		} catch (IOException ioe) {
			ioe.printStackTrace();
		}
	}
}
