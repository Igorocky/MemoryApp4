"use strict";

function Page1({openView,setPageTitle}) {
    const [cnt, setCnt] = useState(0)
    const [state, setState] = useState({name:'111'})

    useEffect(() => {
        be.add(26, {name:'qaz'}).then(res => setCnt(res))
        be.update(state).then(res => setState(res))
        setCnt(2)
    }, [])

    return RE.Container.col.top.center({style:{marginTop:'200px'}},{},
        `This is Page1 [${cnt}]{${state.name}}`,
        RE.Button({variant:"contained", onClick: () => openView(HOME_PAGE_VIEW)}, 'Go to home page'),
    )
}
