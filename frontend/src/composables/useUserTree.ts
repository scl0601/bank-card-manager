import { ref } from 'vue'
import { getUserTreeApi } from '@/api/card'

/**
 * 用户树数据管理 composable
 * 用于获取和处理用户树形数据
 */
export function useUserTree() {
  const loading = ref(false)
  const userTree = ref<any[]>([])

  /**
   * 获取用户树数据
   */
  async function fetchUserTree() {
    loading.value = true
    try {
      const res: any = await getUserTreeApi()
      userTree.value = res.data || []
      return userTree.value
    } finally {
      loading.value = false
    }
  }

  /**
   * 扁平化树形结构
   */
  function flattenTree(tree: any[]): any[] {
    return tree.reduce((acc: any[], item: any) => {
      acc.push(item)
      if (item.children?.length) {
        acc.push(...flattenTree(item.children))
      }
      return acc
    }, [])
  }

  /**
   * 根据条件过滤树形数据
   */
  function filterTree(tree: any[], predicate: (node: any) => boolean): any[] {
    return tree.reduce((acc: any[], node: any) => {
      const match = predicate(node)
      const filteredChildren = node.children ? filterTree(node.children, predicate) : []

      if (match || filteredChildren.length > 0) {
        acc.push({
          ...node,
          children: filteredChildren.length > 0 ? filteredChildren : node.children
        })
      }
      return acc
    }, [])
  }

  return {
    loading,
    userTree,
    fetchUserTree,
    flattenTree,
    filterTree
  }
}
