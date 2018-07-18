package Serializer.utils;

public class IdentityKeyWrapper
{
    private Object m_key;

    public IdentityKeyWrapper(Object key)
    {
        m_key = key;
    }

    public Object getKey()
    {
        return m_key;
    }

    public boolean equals(Object obj)
    {
        if (obj instanceof IdentityKeyWrapper)
        {
            return ((IdentityKeyWrapper)obj).m_key == m_key;
        }
        return false;
    }

    public int hashCode()
    {
        return System.identityHashCode(m_key);
    }
}